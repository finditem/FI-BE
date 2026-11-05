package com.fmi.domain.place.service.result;

import java.util.List;

public record AdminPlaceListResult(List<AdminPlaceResult> places, boolean hasNext) {}
