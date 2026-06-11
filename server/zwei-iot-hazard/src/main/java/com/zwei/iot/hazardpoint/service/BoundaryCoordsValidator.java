package com.zwei.iot.hazardpoint.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.constant.HttpStatus;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.hazardpoint.domain.dto.BoundaryCoordsDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * boundary_coords JSON 校验器。
 */
@Component
public class BoundaryCoordsValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final BigDecimal MIN_LAT = new BigDecimal("-90");
    private static final BigDecimal MAX_LAT = new BigDecimal("90");
    private static final BigDecimal MIN_LNG = new BigDecimal("-180");
    private static final BigDecimal MAX_LNG = new BigDecimal("180");
    private static final int MAX_POLYGON_SIZE = 1000;
    private static final int MAX_AUX_LINES = 50;

    public BoundaryCoordsDTO parseAndValidate(String json) {
        BoundaryCoordsDTO dto;
        try {
            dto = MAPPER.readValue(json, BoundaryCoordsDTO.class);
        } catch (Exception e) {
            throw new ServiceException("boundary_coords: invalid JSON", HttpStatus.BAD_REQUEST);
        }
        validate(dto);
        return dto;
    }

    public void validate(BoundaryCoordsDTO dto) {
        validatePolygon(dto.polygon());
        validateStrikeLine(dto.strikeLine());
        validateAuxiliaryLines(dto.auxiliaryLines());
    }

    private void validatePolygon(List<List<BigDecimal>> polygon) {
        if (polygon == null || polygon.isEmpty()) return;
        if (polygon.size() < 3) {
            throw new ServiceException("boundary_coords: polygon must have >= 3 vertices", HttpStatus.BAD_REQUEST);
        }
        if (polygon.size() > MAX_POLYGON_SIZE) {
            throw new ServiceException("boundary_coords: polygon size exceeds " + MAX_POLYGON_SIZE, HttpStatus.BAD_REQUEST);
        }
        for (int i = 0; i < polygon.size(); i++) {
            validateVertex(polygon.get(i), "polygon[" + i + "]");
        }
    }

    private void validateStrikeLine(List<List<BigDecimal>> strikeLine) {
        if (strikeLine == null) return;
        if (strikeLine.size() != 2) {
            throw new ServiceException("boundary_coords: strikeLine must have exactly 2 points", HttpStatus.BAD_REQUEST);
        }
        validateVertex(strikeLine.get(0), "strikeLine[0]");
        validateVertex(strikeLine.get(1), "strikeLine[1]");
    }

    private void validateAuxiliaryLines(List<List<List<BigDecimal>>> lines) {
        if (lines == null || lines.isEmpty()) return;
        if (lines.size() > MAX_AUX_LINES) {
            throw new ServiceException("boundary_coords: auxiliaryLines size exceeds " + MAX_AUX_LINES, HttpStatus.BAD_REQUEST);
        }
        for (int i = 0; i < lines.size(); i++) {
            List<List<BigDecimal>> line = lines.get(i);
            if (line.size() < 2) {
                throw new ServiceException(
                    "boundary_coords: auxiliaryLine #" + (i + 1) + " must have >= 2 vertices",
                    HttpStatus.BAD_REQUEST
                );
            }
            for (int j = 0; j < line.size(); j++) {
                validateVertex(line.get(j), "auxiliaryLine[" + i + "][" + j + "]");
            }
        }
    }

    private void validateVertex(List<BigDecimal> vertex, String label) {
        if (vertex == null || vertex.size() != 2) {
            throw new ServiceException("boundary_coords: " + label + " must be [lat,lng]", HttpStatus.BAD_REQUEST);
        }
        BigDecimal lat = vertex.get(0);
        BigDecimal lng = vertex.get(1);
        if (lat == null || lat.compareTo(MIN_LAT) < 0 || lat.compareTo(MAX_LAT) > 0) {
            throw new ServiceException("boundary_coords: " + label + " lat out of range [-90,90]", HttpStatus.BAD_REQUEST);
        }
        if (lng == null || lng.compareTo(MIN_LNG) < 0 || lng.compareTo(MAX_LNG) > 0) {
            throw new ServiceException("boundary_coords: " + label + " lng out of range [-180,180]", HttpStatus.BAD_REQUEST);
        }
    }
}
