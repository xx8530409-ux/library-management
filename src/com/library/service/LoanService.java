package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.LoanDAO;
import com.library.dao.MemberDAO;
import com.library.exception.AlreadyRenewedException;
import com.library.exception.BookNotAvailableException;
import com.library.exception.BorrowLimitExceededException;
import com.library.exception.EntityNotFoundException;
import com.library.exception.LoanOverdueException;
import com.library.exception.OverdueBlockException;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.MemberRankingRow;
import com.library.model.OverdueReportRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 借閱業務邏輯（F3 借書、F4 還書、續借）與報表（F6）。
 * <p>本層是業務規則的落腳處：借書檢查、續借檢查、到期日計算、
 * 逾期罰金結算全部集中在此。
 */
public class LoanService {

    private final BookDAO bookDao;
    private final MemberDAO memberDao;
    private final LoanDAO loanDao;

    public LoanService(BookDAO bookDao, MemberDAO memberDao, LoanDAO loanDao) {
        this.bookDao = bookDao;
        this.memberDao = memberDao;
        this.loanDao = loanDao;
    }

    /**
     * 借書（F3）。依序通過三道檢查後才寫入借閱紀錄並扣減份數。
     *
     * @throws BookNotAvailableException     已無可借份數
     * @throws BorrowLimitExceededException  達同時借書上限
     * @throws OverdueBlockException         有逾期未還書籍
     */
    public Loan borrow(long bookId, long memberId) {
        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("找不到書籍 id=" + bookId));
        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("找不到會員 id=" + memberId));

        // 檢查 1：可借份數（電子書不限份數）
        if (!book.hasAvailableCopy()) { // 份數不夠拋出錯誤
            throw new BookNotAvailableException(book);
        }
        // 檢查 2：同時借書上限
        if (loanDao.countActiveByMember(memberId) >= member.borrowLimit()) { // 某會員已借多少書 >= 借閱上限
            throw new BorrowLimitExceededException(member);
        }
        // 檢查 3：逾期封鎖
        if (loanDao.hasOverdue(memberId)) {
            throw new OverdueBlockException(member);
        }

        // 都通過後就寫入借閱紀錄並扣減份數

        // 到期日 = 今天 + 書的借期 + 會員延長天數
        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(book.loanDays() + member.extraDays());

        // 先寫紀錄再扣份數；任一檢查失敗都不會走到這裡，不留半套狀態
        Loan loan = loanDao.insert(new Loan(bookId, memberId, today, due));
        if (book.getType().isCopyLimited()) {
            bookDao.decrementAvailable(bookId);
        }
        return loan;
    }

    /**
     * 還書（F4）。計算逾期天數與罰金，更新紀錄並回補份數。
     *
     * @param loanId 借閱紀錄 id
     * @throws EntityNotFoundException 找不到未歸還的借閱紀錄
     */
    public ReturnResult returnBook(long loanId) {
        Loan loan = loanDao.findActiveById(loanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到未歸還的借閱紀錄 id=" + loanId));
        Book book = bookDao.findById(loan.getBookId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "借閱對應的書籍不存在 id=" + loan.getBookId()));

        LocalDate today = LocalDate.now(); // 找出今天日期看逾期幾天
        long overdueDays = loan.overdueDays(today);

        // 罰金 = 逾期天數 × 類型費率（電子書費率為 0）
        BigDecimal fine = BigDecimal.valueOf(overdueDays * (long) book.finePerDay());

        loan.close(today, fine); // 讓 loanDao.update 去修改
        loanDao.update(loan);

        // 電子書不占份數，不需回補
        if (book.getType().isCopyLimited()) {
            bookDao.incrementAvailable(book.getId());
        }
        return new ReturnResult(loan, overdueDays, fine);
    }

    /**
     * 續借書籍。
     * <p>檢核條件：
     * 1. 該借閱單未逾期（today <= dueDate）
     * 2. 會員名下無其他逾期未還書籍
     * 3. 該借閱單尚未續借過（每筆借閱限續借一次）
     * <p>展延計算：原到期日 (dueDate) + 會員身分續借天數。
     *
     * @param loanId 借閱紀錄 id
     * @return 續借更新後的 Loan
     * @throws EntityNotFoundException 找不到未歸還的借閱紀錄
     * @throws LoanOverdueException    該書已逾期
     * @throws OverdueBlockException   會員有逾期書籍遭封鎖
     * @throws AlreadyRenewedException 該紀錄已續借過
     */
    public Loan renew(long loanId) {
        Loan loan = loanDao.findActiveById(loanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到未歸還的借閱紀錄 id=" + loanId));

        LocalDate today = LocalDate.now();

        // 檢核 1：該筆借閱是否已逾期（已到期不可續借）
        if (loan.isOverdue(today)) {
            throw new LoanOverdueException(loanId);
        }

        // 檢核 2：會員名下是否有任何逾期書籍（有逾期不可續借）
        if (loanDao.hasOverdue(loan.getMemberId())) {
            Member member = memberDao.findById(loan.getMemberId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "找不到會員 id=" + loan.getMemberId()));
            throw new OverdueBlockException(member, "續借");
        }

        // 檢核 3：每筆限續借一次
        if (loan.getRenewCount() >= 1) {
            throw new AlreadyRenewedException(loanId);
        }

        // 取得會員身分設定的續借天數（目前統一為 5 天）
        Member member = memberDao.findById(loan.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "找不到會員 id=" + loan.getMemberId()));
        int renewalDays = member.renewalDays();

        // 依「原到期日 + 續借天數」展延，並累計續借次數
        loan.renew(renewalDays);
        loanDao.updateRenewal(loan);
        return loan;
    }

    /** 全部未歸還的借閱紀錄。 */
    public List<Loan> listActiveLoans() {
        return loanDao.findAllActive();
    }

    /** 逾期借閱清單（F6），按逾期天數排序。 */
    public List<OverdueReportRow> overdueReport() {
        return loanDao.overdueReport();
    }

    /** 會員借閱排行（F6），預設前 10 名。 */
    public List<MemberRankingRow> memberRanking() {
        return loanDao.memberRanking(10);
    }
}
