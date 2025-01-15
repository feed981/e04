package com.feddoubt.model.YT1.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LocationInfoDto {
  private BigDecimal latitude;
  private BigDecimal longitude;
}