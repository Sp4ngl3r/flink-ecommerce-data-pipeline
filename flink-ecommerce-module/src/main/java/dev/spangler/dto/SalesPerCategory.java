package dev.spangler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Date;

@Data
@AllArgsConstructor
public class SalesPerCategory {

    private Date transactionDate;
    private String category;
    private double totalSales;
}
