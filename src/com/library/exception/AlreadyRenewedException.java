package com.library.exception;

/**
 * 借閱紀錄重複申請續借時拋出（每筆借閱限續借一次）。
 */
public class AlreadyRenewedException extends LibraryException {

    public AlreadyRenewedException(long loanId) {
        super("借閱單 #" + loanId + " 已經續借過，每筆借閱限續借一次");
    }
}
