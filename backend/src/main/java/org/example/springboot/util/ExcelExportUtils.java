package org.example.springboot.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.springboot.entity.House;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Excel导出工具类
 */
public class ExcelExportUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 导出交易记录到Excel
     */
    public static byte[] exportTransactionsToExcel(List<Transaction> transactions, Map<String, Object> statistics) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle incomeStyle = createIncomeStyle(workbook);
            CellStyle expenseStyle = createExpenseStyle(workbook);
            CellStyle summaryHeaderStyle = createSummaryHeaderStyle(workbook);
            CellStyle summaryDataStyle = createSummaryDataStyle(workbook);

            // 创建一个综合Sheet，包含统计汇总和交易明细
            Sheet mainSheet = workbook.createSheet("收支明细报表");
            int nextRow = createSummarySection(mainSheet, statistics, summaryHeaderStyle, summaryDataStyle);

            // 添加空行分隔
            nextRow += 2;

            // 创建交易明细部分
            createDetailSection(mainSheet, transactions, headerStyle, dataStyle, incomeStyle, expenseStyle, nextRow);

            // 自动调整列宽
            autoSizeColumns(mainSheet, 7);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建统计汇总部分（返回下一行的行号）
     */
    private static int createSummarySection(Sheet sheet, Map<String, Object> statistics,
                                           CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;

        // 标题
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("交易统计报表");
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);

        // 空行
        rowNum++;

        // 统计周期
        Row periodRow = sheet.createRow(rowNum++);
        Cell periodLabelCell = periodRow.createCell(0);
        periodLabelCell.setCellValue("统计周期：");
        periodLabelCell.setCellStyle(headerStyle);
        Cell periodValueCell = periodRow.createCell(1);
        String period = (String) statistics.get("period");
        String periodText = "all".equals(period) ? "全部" :
                           "month".equals(period) ? "本月" :
                           "year".equals(period) ? "本年" : period;
        periodValueCell.setCellValue(periodText);
        periodValueCell.setCellStyle(dataStyle);

        // 筛选条件 - 交易类型
        Integer filterType = (Integer) statistics.get("filterType");
        if (filterType != null) {
            Row filterTypeRow = sheet.createRow(rowNum++);
            Cell filterTypeLabelCell = filterTypeRow.createCell(0);
            filterTypeLabelCell.setCellValue("交易类型筛选：");
            filterTypeLabelCell.setCellStyle(headerStyle);
            Cell filterTypeValueCell = filterTypeRow.createCell(1);
            filterTypeValueCell.setCellValue(filterType == 1 ? "仅收入" : "仅支出");
            filterTypeValueCell.setCellStyle(dataStyle);
        }

        // 筛选条件 - 用户名
        String filterUsername = (String) statistics.get("filterUsername");
        if (filterUsername != null && !filterUsername.isEmpty()) {
            Row filterUsernameRow = sheet.createRow(rowNum++);
            Cell filterUsernameLabelCell = filterUsernameRow.createCell(0);
            filterUsernameLabelCell.setCellValue("用户名筛选：");
            filterUsernameLabelCell.setCellStyle(headerStyle);
            Cell filterUsernameValueCell = filterUsernameRow.createCell(1);
            filterUsernameValueCell.setCellValue(filterUsername);
            filterUsernameValueCell.setCellStyle(dataStyle);
        }

        // 空行
        rowNum++;

        // 收入统计
        Row incomeRow = sheet.createRow(rowNum++);
        Cell incomeLabelCell = incomeRow.createCell(0);
        incomeLabelCell.setCellValue("总收入：");
        incomeLabelCell.setCellStyle(headerStyle);
        Cell incomeValueCell = incomeRow.createCell(1);
        BigDecimal totalIncome = (BigDecimal) statistics.get("totalIncome");
        incomeValueCell.setCellValue("¥ " + (totalIncome != null ? totalIncome.toString() : "0.00"));
        CellStyle incomeDataStyle = sheet.getWorkbook().createCellStyle();
        incomeDataStyle.cloneStyleFrom(dataStyle);
        Font incomeFont = sheet.getWorkbook().createFont();
        incomeFont.setColor(IndexedColors.GREEN.getIndex());
        incomeFont.setBold(true);
        incomeDataStyle.setFont(incomeFont);
        incomeValueCell.setCellStyle(incomeDataStyle);

        // 收入笔数
        Row incomeCountRow = sheet.createRow(rowNum++);
        Cell incomeCountLabelCell = incomeCountRow.createCell(0);
        incomeCountLabelCell.setCellValue("收入笔数：");
        incomeCountLabelCell.setCellStyle(headerStyle);
        Cell incomeCountValueCell = incomeCountRow.createCell(1);
        incomeCountValueCell.setCellValue(statistics.get("incomeCount") + " 笔");
        incomeCountValueCell.setCellStyle(dataStyle);

        // 空行
        rowNum++;

        // 支出统计
        Row expenseRow = sheet.createRow(rowNum++);
        Cell expenseLabelCell = expenseRow.createCell(0);
        expenseLabelCell.setCellValue("总支出：");
        expenseLabelCell.setCellStyle(headerStyle);
        Cell expenseValueCell = expenseRow.createCell(1);
        BigDecimal totalExpense = (BigDecimal) statistics.get("totalExpense");
        expenseValueCell.setCellValue("¥ " + (totalExpense != null ? totalExpense.toString() : "0.00"));
        CellStyle expenseDataStyle = sheet.getWorkbook().createCellStyle();
        expenseDataStyle.cloneStyleFrom(dataStyle);
        Font expenseFont = sheet.getWorkbook().createFont();
        expenseFont.setColor(IndexedColors.RED.getIndex());
        expenseFont.setBold(true);
        expenseDataStyle.setFont(expenseFont);
        expenseValueCell.setCellStyle(expenseDataStyle);

        // 支出笔数
        Row expenseCountRow = sheet.createRow(rowNum++);
        Cell expenseCountLabelCell = expenseCountRow.createCell(0);
        expenseCountLabelCell.setCellValue("支出笔数：");
        expenseCountLabelCell.setCellStyle(headerStyle);
        Cell expenseCountValueCell = expenseCountRow.createCell(1);
        expenseCountValueCell.setCellValue(statistics.get("expenseCount") + " 笔");
        expenseCountValueCell.setCellStyle(dataStyle);

        // 空行
        rowNum++;

        // 余额
        Row balanceRow = sheet.createRow(rowNum++);
        Cell balanceLabelCell = balanceRow.createCell(0);
        balanceLabelCell.setCellValue("收支结余：");
        balanceLabelCell.setCellStyle(headerStyle);
        Cell balanceValueCell = balanceRow.createCell(1);
        BigDecimal balance = (BigDecimal) statistics.get("balance");
        balanceValueCell.setCellValue("¥ " + (balance != null ? balance.toString() : "0.00"));
        CellStyle balanceDataStyle = sheet.getWorkbook().createCellStyle();
        balanceDataStyle.cloneStyleFrom(dataStyle);
        Font balanceFont = sheet.getWorkbook().createFont();
        balanceFont.setBold(true);
        balanceDataStyle.setFont(balanceFont);
        balanceValueCell.setCellStyle(balanceDataStyle);

        // 总交易笔数
        Row totalCountRow = sheet.createRow(rowNum++);
        Cell totalCountLabelCell = totalCountRow.createCell(0);
        totalCountLabelCell.setCellValue("总交易笔数：");
        totalCountLabelCell.setCellStyle(headerStyle);
        Cell totalCountValueCell = totalCountRow.createCell(1);
        totalCountValueCell.setCellValue(statistics.get("totalCount") + " 笔");
        totalCountValueCell.setCellStyle(dataStyle);

        return rowNum;
    }

    /**
     * 创建交易明细部分
     */
    private static void createDetailSection(Sheet sheet, List<Transaction> transactions,
                                           CellStyle headerStyle, CellStyle dataStyle,
                                           CellStyle incomeStyle, CellStyle expenseStyle, int startRow) {
        int rowNum = startRow;

        // 明细标题
        Row detailTitleRow = sheet.createRow(rowNum++);
        Cell detailTitleCell = detailTitleRow.createCell(0);
        detailTitleCell.setCellValue("交易明细");
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        detailTitleCell.setCellStyle(titleStyle);

        // 空行
        rowNum++;

        // 创建表头
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"交易ID", "订单编号", "用户名", "交易类型", "交易金额", "交易描述", "交易时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        for (Transaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);

            // 交易ID
            Cell idCell = row.createCell(0);
            idCell.setCellValue(transaction.getId());
            idCell.setCellStyle(dataStyle);

            // 订单编号
            Cell orderNoCell = row.createCell(1);
            if (transaction.getOrder() != null) {
                orderNoCell.setCellValue(transaction.getOrder().getOrderNo());
            } else {
                orderNoCell.setCellValue("-");
            }
            orderNoCell.setCellStyle(dataStyle);

            // 用户名
            Cell usernameCell = row.createCell(2);
            if (transaction.getUser() != null) {
                usernameCell.setCellValue(transaction.getUser().getUsername());
            } else {
                usernameCell.setCellValue("-");
            }
            usernameCell.setCellStyle(dataStyle);

            // 交易类型
            Cell typeCell = row.createCell(3);
            typeCell.setCellValue(transaction.getType() == 1 ? "收入" : "支出");
            typeCell.setCellStyle(transaction.getType() == 1 ? incomeStyle : expenseStyle);

            // 交易金额
            Cell amountCell = row.createCell(4);
            String prefix = transaction.getType() == 1 ? "+" : "-";
            amountCell.setCellValue(prefix + " ¥ " + transaction.getAmount().toString());
            amountCell.setCellStyle(transaction.getType() == 1 ? incomeStyle : expenseStyle);

            // 交易描述
            Cell descCell = row.createCell(5);
            descCell.setCellValue(transaction.getDescription() != null ? transaction.getDescription() : "-");
            descCell.setCellStyle(dataStyle);

            // 交易时间
            Cell timeCell = row.createCell(6);
            if (transaction.getCreateTime() != null) {
                timeCell.setCellValue(transaction.getCreateTime().format(DATE_FORMATTER));
            } else {
                timeCell.setCellValue("-");
            }
            timeCell.setCellStyle(dataStyle);
        }
    }

    /**
     * 创建表头样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }

    /**
     * 创建数据样式
     */
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 创建收入样式
     */
    private static CellStyle createIncomeStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.GREEN.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * 创建支出样式
     */
    private static CellStyle createExpenseStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * 创建统计表头样式
     */
    private static CellStyle createSummaryHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    /**
     * 创建统计数据样式
     */
    private static CellStyle createSummaryDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    /**
     * 自动调整列宽
     */
    private static void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // 设置最小宽度和最大宽度
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth < 3000) {
                sheet.setColumnWidth(i, 3000);
            } else if (currentWidth > 15000) {
                sheet.setColumnWidth(i, 15000);
            }
        }
    }

    /**
     * 导出房源列表到Excel
     */
    public static byte[] exportHousesToExcel(List<House> houses) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 创建房源列表Sheet
            Sheet sheet = workbook.createSheet("房源列表");
            createHousesSheet(sheet, houses, headerStyle, dataStyle);

            // 自动调整列宽
            autoSizeColumns(sheet, 11);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建房源列表Sheet
     */
    private static void createHousesSheet(Sheet sheet, List<House> houses,
                                         CellStyle headerStyle, CellStyle dataStyle) {
        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"房源ID", "房源标题", "地址", "面积(㎡)", "价格(元/月)",
                           "房屋类型", "房东姓名", "状态", "平均评分", "评论数", "发布时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (House house : houses) {
            Row row = sheet.createRow(rowNum++);

            // 房源ID
            Cell idCell = row.createCell(0);
            idCell.setCellValue(house.getId());
            idCell.setCellStyle(dataStyle);

            // 房源标题
            Cell titleCell = row.createCell(1);
            titleCell.setCellValue(house.getTitle() != null ? house.getTitle() : "-");
            titleCell.setCellStyle(dataStyle);

            // 地址
            Cell addressCell = row.createCell(2);
            addressCell.setCellValue(house.getAddress() != null ? house.getAddress() : "-");
            addressCell.setCellStyle(dataStyle);

            // 面积
            Cell areaCell = row.createCell(3);
            if (house.getArea() != null) {
                areaCell.setCellValue(house.getArea().toString());
            } else {
                areaCell.setCellValue("-");
            }
            areaCell.setCellStyle(dataStyle);

            // 价格
            Cell priceCell = row.createCell(4);
            if (house.getPrice() != null) {
                priceCell.setCellValue("¥ " + house.getPrice().toString());
            } else {
                priceCell.setCellValue("-");
            }
            priceCell.setCellStyle(dataStyle);

            // 房屋类型
            Cell typeCell = row.createCell(5);
            typeCell.setCellValue(house.getTypeName() != null ? house.getTypeName() : "-");
            typeCell.setCellStyle(dataStyle);

            // 房东姓名
            Cell landlordCell = row.createCell(6);
            landlordCell.setCellValue(house.getLandlordName() != null ? house.getLandlordName() : "-");
            landlordCell.setCellStyle(dataStyle);

            // 状态
            Cell statusCell = row.createCell(7);
            String statusText = house.getStatus() == null ? "-" :
                              house.getStatus() == 0 ? "下架" :
                              house.getStatus() == 1 ? "待出租" : "已出租";
            statusCell.setCellValue(statusText);
            statusCell.setCellStyle(dataStyle);

            // 平均评分
            Cell scoreCell = row.createCell(8);
            if (house.getAvgScore() != null) {
                scoreCell.setCellValue(house.getAvgScore().toString());
            } else {
                scoreCell.setCellValue("-");
            }
            scoreCell.setCellStyle(dataStyle);

            // 评论数
            Cell commentCell = row.createCell(9);
            commentCell.setCellValue(house.getCommentCount() != null ? house.getCommentCount().toString() : "0");
            commentCell.setCellStyle(dataStyle);

            // 发布时间
            Cell timeCell = row.createCell(10);
            if (house.getCreateTime() != null) {
                timeCell.setCellValue(house.getCreateTime().format(DATE_FORMATTER));
            } else {
                timeCell.setCellValue("-");
            }
            timeCell.setCellStyle(dataStyle);
        }
    }

    /**
     * 导出订单列表到Excel
     */
    public static byte[] exportOrdersToExcel(List<Order> orders) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            // 创建订单列表Sheet
            Sheet sheet = workbook.createSheet("订单列表");
            createOrdersSheet(sheet, orders, headerStyle, dataStyle);

            // 自动调整列宽
            autoSizeColumns(sheet, 10);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 创建订单列表Sheet
     */
    private static void createOrdersSheet(Sheet sheet, List<Order> orders,
                                         CellStyle headerStyle, CellStyle dataStyle) {
        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"订单ID", "订单编号", "房源标题", "租客姓名", "房东姓名",
                           "订单金额", "押金金额", "支付方式", "订单状态", "创建时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 填充数据
        int rowNum = 1;
        for (Order order : orders) {
            Row row = sheet.createRow(rowNum++);

            // 订单ID
            Cell idCell = row.createCell(0);
            idCell.setCellValue(order.getId());
            idCell.setCellStyle(dataStyle);

            // 订单编号
            Cell orderNoCell = row.createCell(1);
            orderNoCell.setCellValue(order.getOrderNo() != null ? order.getOrderNo() : "-");
            orderNoCell.setCellStyle(dataStyle);

            // 房源标题
            Cell houseCell = row.createCell(2);
            if (order.getHouse() != null && order.getHouse().getTitle() != null) {
                houseCell.setCellValue(order.getHouse().getTitle());
            } else {
                houseCell.setCellValue("-");
            }
            houseCell.setCellStyle(dataStyle);

            // 租客姓名
            Cell tenantCell = row.createCell(3);
            if (order.getTenant() != null && order.getTenant().getName() != null) {
                tenantCell.setCellValue(order.getTenant().getName());
            } else {
                tenantCell.setCellValue("-");
            }
            tenantCell.setCellStyle(dataStyle);

            // 房东姓名
            Cell landlordCell = row.createCell(4);
            if (order.getLandlord() != null && order.getLandlord().getName() != null) {
                landlordCell.setCellValue(order.getLandlord().getName());
            } else {
                landlordCell.setCellValue("-");
            }
            landlordCell.setCellStyle(dataStyle);

            // 订单金额
            Cell amountCell = row.createCell(5);
            if (order.getAmount() != null) {
                amountCell.setCellValue("¥ " + order.getAmount().toString());
            } else {
                amountCell.setCellValue("-");
            }
            amountCell.setCellStyle(dataStyle);

            // 押金金额
            Cell depositCell = row.createCell(6);
            if (order.getDeposit() != null) {
                depositCell.setCellValue("¥ " + order.getDeposit().toString());
            } else {
                depositCell.setCellValue("-");
            }
            depositCell.setCellStyle(dataStyle);

            // 支付方式
            Cell paymentCell = row.createCell(7);
            paymentCell.setCellValue(order.getPaymentMethod() != null ? order.getPaymentMethod() : "-");
            paymentCell.setCellStyle(dataStyle);

            // 订单状态
            Cell statusCell = row.createCell(8);
            String statusText = order.getStatus() == null ? "-" :
                              order.getStatus() == 0 ? "待支付" :
                              order.getStatus() == 1 ? "已支付待确认" :
                              order.getStatus() == 2 ? "已确认" :
                              order.getStatus() == 3 ? "已取消" : "已退款";
            statusCell.setCellValue(statusText);
            statusCell.setCellStyle(dataStyle);

            // 创建时间
            Cell timeCell = row.createCell(9);
            if (order.getCreateTime() != null) {
                timeCell.setCellValue(order.getCreateTime().format(DATE_FORMATTER));
            } else {
                timeCell.setCellValue("-");
            }
            timeCell.setCellStyle(dataStyle);
        }
    }
}
