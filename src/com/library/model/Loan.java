package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 借閱紀錄實體，對應資料表 {@code loans} 的一列。
 * <p>{@code returnDate == null} 代表尚未歸還（仍在借閱中）。
 * 逾期判斷、逾期天數與續借業務邏輯封裝於本類別。
 */
public class Loan {

    private long id;
    private long bookId;
    private long memberId;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;   // null 代表未還
    private BigDecimal fine = BigDecimal.ZERO;
    private int renewCount = 0;     // 續借次數，限 1 次

    public Loan() {
    }

    /** 新借出用：借閱日與到期日由 Service 計算後帶入。 */
    public Loan(long bookId, long memberId, LocalDate loanDate, LocalDate dueDate) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
    }

    /** 從資料庫載入用：帶基本完整欄位（預設 renewCount = 0）。 */
    public Loan(long id, long bookId, long memberId, LocalDate loanDate,
                LocalDate dueDate, LocalDate returnDate, BigDecimal fine) {
        this(id, bookId, memberId, loanDate, dueDate, returnDate, fine, 0);
    }

    /** 從資料庫載入用：帶全部欄位（含 renewCount）。 */
    public Loan(long id, long bookId, long memberId, LocalDate loanDate,
                LocalDate dueDate, LocalDate returnDate, BigDecimal fine, int renewCount) {
        this.id = id;
        this.bookId = bookId;
        this.memberId = memberId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fine = fine == null ? BigDecimal.ZERO : fine;
        this.renewCount = renewCount;
    }

    // ── 業務行為 ─────────────────────────────────────────────

    /** 是否尚未歸還。 */
    public boolean isActive() {
        return returnDate == null; // 有值代表已歸還
    }

    /**
     * 相對於指定日期是否已逾期（僅在尚未歸還時成立）。
     * @param asOf 判斷基準日，通常為今天
     */
    public boolean isOverdue(LocalDate asOf) { // 是否過期
        return isActive() && asOf.isAfter(dueDate);
    }

    /**
     * 相對於指定日期的逾期天數；未逾期回傳 0。
     * @param asOf 判斷基準日，通常為今天
     */
    public long overdueDays(LocalDate asOf) {
        long days = ChronoUnit.DAYS.between(dueDate, asOf);
        return Math.max(0, days);
    }

    /**
     * 是否符合續借資格（未歸還、基準日未逾期、且尚未續借過）。
     * @param asOf 判斷基準日，通常為今天
     */
    public boolean canRenew(LocalDate asOf) {
        return isActive() && !isOverdue(asOf) && renewCount < 1;
    }

    /**
     * 執行續借：依原到期日累加天數，並累計續借次數。
     * @param extraDays 續借天數
     */
    public void renew(int extraDays) {
        this.dueDate = this.dueDate.plusDays(extraDays);
        this.renewCount++;
    }

    /**
     * 結算歸還：設定歸還日與罰金。
     * @param returnDate 歸還日期
     * @param fine       結算後的罰金金額
     */
    public void close(LocalDate returnDate, BigDecimal fine) {
        this.returnDate = returnDate;
        this.fine = fine == null ? BigDecimal.ZERO : fine;
    }

    // ── getter / setter ─────────────────────────────────────

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBookId() {
        return bookId;
    }

    public void setBookId(long bookId) {
        this.bookId = bookId;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public void setFine(BigDecimal fine) {
        this.fine = fine == null ? BigDecimal.ZERO : fine;
    }

    public int getRenewCount() {
        return renewCount;
    }

    public void setRenewCount(int renewCount) {
        this.renewCount = renewCount;
    }
}
