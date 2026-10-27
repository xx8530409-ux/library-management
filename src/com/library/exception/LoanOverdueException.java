package com.library.exception;

/**
 * 借閱書籍已逾期仍申請續借時拋出（逾期書籍禁止續借）。
 */
public class LoanOverdueException extends LibraryException {

    public LoanOverdueException(long loanId) {
        super("借閱單 #" + loanId + " 已經逾期，無法辦理續借，請盡速歸還並結算罰金");
    }
}
