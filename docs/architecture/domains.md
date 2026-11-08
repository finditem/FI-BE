# 도메인 아키텍처

## 주요 도메인

- **Post** — 분실물, 습득물 게시글 등록 및 조회
- **Favorite** — 게시글 즐겨찾기
- **User / Auth** — 회원가입, 로그인, JWT 인증
- **Map** — 지도 기반 위치 검색
- **Chat** — 채팅방, 채팅 메시지 (WebSocket)
- **Comment / Like** — 댓글(게시글, 공지) 및 좋아요
- **Notification** — 실시간 알림 (Web Push)
- **Report / Admin** — 신고 처리, 관리자 기능
- **Notice / Inquiry** — 공지사항, 문의

이 목록은 이전 문서의 주요 업무 영역을 요약합니다. 실제 package와 데이터 owner는 현재 FI-BE 구현과 [Domain Boundaries](domain-boundaries.md)를 확인합니다.
