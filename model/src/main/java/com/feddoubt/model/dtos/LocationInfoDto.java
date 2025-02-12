package com.feddoubt.model.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationInfoDto {
  private BigDecimal latitude;
  private BigDecimal longitude;
}