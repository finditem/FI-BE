package com.fmi.external.storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 업로드 이미지에서 촬영 위치, 촬영 시각, 기기 정보 같은 메타데이터를 제거한다.
 *
 * <p>픽셀 데이터는 다시 인코딩하지 않고 그대로 복사한다. 표시에 필요한 정보만 허용 목록으로 남기고 나머지는 모두 버린다.
 * 이미지 끝(JPEG EOI, PNG IEND) 뒤에 붙은 데이터도 버린다.
 */
final class ImageMetadataRemover {

    private static final int PNG_SIGNATURE_LENGTH = 8;

    // 표시에 필요한 PNG chunk. 텍스트(tEXt, zTXt, iTXt), eXIf, tIME 등은 버린다.
    private static final Set<String> PNG_KEPT_CHUNKS = Set.of(
            "IHDR", "PLTE", "IDAT", "IEND", "tRNS", "cHRM", "gAMA", "iCCP", "sBIT", "sRGB", "bKGD", "pHYs", "cICP",
            "mDCv", "cLLi", "acTL", "fcTL", "fdAT");

    private static final int JPEG_SOI = 0xD8;
    private static final int JPEG_EOI = 0xD9;
    private static final int JPEG_SOS = 0xDA;
    private static final int JPEG_APP0 = 0xE0;
    private static final int JPEG_APP1 = 0xE1;
    private static final int JPEG_APP2 = 0xE2;
    private static final int JPEG_APP14 = 0xEE;
    private static final int JPEG_APP15 = 0xEF;
    private static final int JPEG_COM = 0xFE;

    private static final byte[] EXIF_HEADER = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ICC_PROFILE_HEADER = "ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ADOBE_HEADER = "Adobe".getBytes(StandardCharsets.US_ASCII);
    private static final int EXIF_ORIENTATION_TAG = 0x0112;
    private static final int EXIF_SHORT_TYPE = 3;

    private ImageMetadataRemover() {}

    // 구조가 깨진 이미지는 IllegalArgumentException으로 거절한다.
    static byte[] remove(byte[] image, ImageFormat format) {
        return switch (format) {
            case JPEG -> removeFromJpeg(image);
            case PNG -> removeFromPng(image);
        };
    }

    private static byte[] removeFromJpeg(byte[] image) {
        // APP0(JFIF) 다음에 회전 정보를 넣기 위해 남길 구간을 먼저 모은다.
        List<int[]> keptRanges = new ArrayList<>();
        Integer orientation = null;
        int position = 2;

        while (position < image.length) {
            if ((image[position] & 0xFF) != 0xFF || position + 1 >= image.length) {
                throw new IllegalArgumentException("JPEG marker를 읽을 수 없습니다.");
            }
            // marker 앞의 fill byte(0xFF)는 건너뛴다.
            while (position + 1 < image.length && (image[position + 1] & 0xFF) == 0xFF) {
                position++;
            }
            if (position + 1 >= image.length) {
                throw new IllegalArgumentException("JPEG marker를 읽을 수 없습니다.");
            }
            int marker = image[position + 1] & 0xFF;

            if (marker == JPEG_EOI) {
                keptRanges.add(new int[] {position, position + 2});
                break;
            }
            if (isStandaloneJpegMarker(marker)) {
                keptRanges.add(new int[] {position, position + 2});
                position += 2;
                continue;
            }

            if (position + 4 > image.length) {
                throw new IllegalArgumentException("JPEG segment 길이를 읽을 수 없습니다.");
            }
            int segmentLength = readUnsignedShort(image, position + 2, false);
            int payloadStart = position + 4;
            int segmentEnd = position + 2 + segmentLength;
            if (segmentLength < 2 || segmentEnd > image.length) {
                throw new IllegalArgumentException("JPEG segment 길이가 올바르지 않습니다.");
            }

            if (orientation == null
                    && marker == JPEG_APP1
                    && startsWith(image, payloadStart, segmentEnd, EXIF_HEADER)) {
                orientation = readExifOrientation(image, payloadStart + EXIF_HEADER.length, segmentEnd);
            }
            if (isKeptJpegSegment(image, marker, payloadStart, segmentEnd)) {
                keptRanges.add(new int[] {position, segmentEnd});
            }
            position = segmentEnd;

            if (marker == JPEG_SOS) {
                int scanEnd = findJpegScanEnd(image, position);
                keptRanges.add(new int[] {position, scanEnd});
                position = scanEnd;
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(image.length);
        output.write(0xFF);
        output.write(JPEG_SOI);
        int rangeIndex = 0;
        boolean startsWithApp0 = !keptRanges.isEmpty() && (image[keptRanges.get(0)[0] + 1] & 0xFF) == JPEG_APP0;
        if (startsWithApp0) {
            int[] app0Range = keptRanges.get(0);
            output.write(image, app0Range[0], app0Range[1] - app0Range[0]);
            rangeIndex = 1;
        }
        if (orientation != null && orientation != 1) {
            output.writeBytes(createOrientationOnlyExifSegment(orientation));
        }
        for (; rangeIndex < keptRanges.size(); rangeIndex++) {
            int[] range = keptRanges.get(rangeIndex);
            output.write(image, range[0], range[1] - range[0]);
        }
        return output.toByteArray();
    }

    private static boolean isStandaloneJpegMarker(int marker) {
        return marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7);
    }

    private static boolean isKeptJpegSegment(byte[] image, int marker, int payloadStart, int segmentEnd) {
        if (marker == JPEG_COM) {
            return false;
        }
        if (marker < JPEG_APP0 || marker > JPEG_APP15) {
            return true;
        }
        // APP segment 중 표시에 필요한 JFIF, ICC 색상 프로필, Adobe 색 변환 정보만 남긴다.
        return switch (marker) {
            case JPEG_APP0 -> true;
            case JPEG_APP2 -> startsWith(image, payloadStart, segmentEnd, ICC_PROFILE_HEADER);
            case JPEG_APP14 -> startsWith(image, payloadStart, segmentEnd, ADOBE_HEADER);
            default -> false;
        };
    }

    // 압축 데이터 안의 0xFF는 0x00 또는 RST marker가 뒤따른다. 그 외 marker가 나오면 scan이 끝난다.
    private static int findJpegScanEnd(byte[] image, int scanStart) {
        int position = scanStart;
        while (position + 1 < image.length) {
            if ((image[position] & 0xFF) != 0xFF) {
                position++;
                continue;
            }
            int next = image[position + 1] & 0xFF;
            if (next == 0x00 || (next >= 0xD0 && next <= 0xD7)) {
                position += 2;
                continue;
            }
            return position;
        }
        // EOI 없이 끝난 JPEG은 남은 데이터를 그대로 둔다.
        return image.length;
    }

    // EXIF IFD0의 Orientation 값을 읽는다. 구조가 깨져 있으면 회전 정보 없이 처리한다.
    private static Integer readExifOrientation(byte[] image, int tiffStart, int segmentEnd) {
        if (tiffStart + 8 > segmentEnd) {
            return null;
        }
        boolean littleEndian;
        if (image[tiffStart] == 'I' && image[tiffStart + 1] == 'I') {
            littleEndian = true;
        } else if (image[tiffStart] == 'M' && image[tiffStart + 1] == 'M') {
            littleEndian = false;
        } else {
            return null;
        }

        long ifdOffset = readUnsignedInt(image, tiffStart + 4, littleEndian);
        if (ifdOffset > segmentEnd - tiffStart - 2) {
            return null;
        }
        int ifdStart = tiffStart + (int) ifdOffset;
        int entryCount = readUnsignedShort(image, ifdStart, littleEndian);
        for (int index = 0; index < entryCount; index++) {
            int entryStart = ifdStart + 2 + index * 12;
            if (entryStart + 12 > segmentEnd) {
                return null;
            }
            int tag = readUnsignedShort(image, entryStart, littleEndian);
            int type = readUnsignedShort(image, entryStart + 2, littleEndian);
            if (tag == EXIF_ORIENTATION_TAG && type == EXIF_SHORT_TYPE) {
                int orientation = readUnsignedShort(image, entryStart + 8, littleEndian);
                return orientation >= 1 && orientation <= 8 ? orientation : null;
            }
        }
        return null;
    }

    // Orientation 태그 하나만 담은 EXIF APP1 segment를 만든다.
    private static byte[] createOrientationOnlyExifSegment(int orientation) {
        return new byte[] {
            (byte) 0xFF,
            (byte) JPEG_APP1,
            0x00,
            0x22, // marker, 길이 34
            'E',
            'x',
            'i',
            'f',
            0x00,
            0x00, // EXIF header
            'M',
            'M',
            0x00,
            0x2A,
            0x00,
            0x00,
            0x00,
            0x08, // TIFF header, big endian
            0x00,
            0x01, // IFD0 entry 1개
            0x01,
            0x12,
            0x00,
            0x03,
            0x00,
            0x00,
            0x00,
            0x01,
            0x00,
            (byte) orientation,
            0x00,
            0x00, // Orientation
            0x00,
            0x00,
            0x00,
            0x00 // 다음 IFD 없음
        };
    }

    private static byte[] removeFromPng(byte[] image) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(image.length);
        output.write(image, 0, PNG_SIGNATURE_LENGTH);
        int position = PNG_SIGNATURE_LENGTH;

        while (position + 8 <= image.length) {
            long dataLength = readUnsignedInt(image, position, false);
            if (dataLength > image.length - position - 12L) {
                throw new IllegalArgumentException("PNG chunk 길이가 올바르지 않습니다.");
            }
            String chunkType = new String(image, position + 4, 4, StandardCharsets.US_ASCII);
            int chunkEnd = position + 12 + (int) dataLength;

            if (PNG_KEPT_CHUNKS.contains(chunkType)) {
                output.write(image, position, chunkEnd - position);
            }
            if (chunkType.equals("IEND")) {
                return output.toByteArray();
            }
            position = chunkEnd;
        }
        throw new IllegalArgumentException("PNG IEND chunk가 없습니다.");
    }

    private static boolean startsWith(byte[] image, int start, int end, byte[] prefix) {
        return end - start >= prefix.length
                && Arrays.equals(image, start, start + prefix.length, prefix, 0, prefix.length);
    }

    private static int readUnsignedShort(byte[] image, int offset, boolean littleEndian) {
        int first = image[offset] & 0xFF;
        int second = image[offset + 1] & 0xFF;
        return littleEndian ? (second << 8) | first : (first << 8) | second;
    }

    private static long readUnsignedInt(byte[] image, int offset, boolean littleEndian) {
        long value = 0;
        for (int index = 0; index < 4; index++) {
            int byteIndex = littleEndian ? offset + 3 - index : offset + index;
            value = (value << 8) | (image[byteIndex] & 0xFF);
        }
        return value;
    }
}
