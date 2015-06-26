package com.mopub.common.event;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Immutable data class with error event data.
 */
public class ErrorEvent extends BaseEvent {
    private final String mErrorExceptionClassName;
    private final String mErrorMessage;
    private final String mErrorStackTrace;
    private final String mErrorFileName;
    private final String mErrorClassName;
    private final String mErrorMethodName;
    private final Integer mErrorLineNumber;

    private ErrorEvent(Builder builder) {
        super(builder);
        mErrorExceptionClassName = builder.mErrorExceptionClassName;
        mErrorMessage = builder.mErrorMessage;
        mErrorStackTrace = builder.mErrorStackTrace;
        mErrorFileName = builder.mErrorFileName;
        mErrorClassName = builder.mErrorClassName;
        mErrorMethodName = builder.mErrorMethodName;
        mErrorLineNumber = builder.mErrorLineNumber;
    }

    public String getErrorExceptionClassName() {
        return mErrorExceptionClassName;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public String getErrorStackTrace() {
        return mErrorStackTrace;
    }

    public String getErrorFileName() {
        return mErrorFileName;
    }

    public String getErrorClassName() {
        return mErrorClassName;
    }

    public String getErrorMethodName() {
        return mErrorMethodName;
    }

    public Integer getErrorLineNumber() {
        return mErrorLineNumber;
    }

    @Override
    public String toString() {
        final String string = super.toString();
        return string +
                "ErrorEvent\n" +
                "ErrorExceptionClassName: " + getErrorExceptionClassName() + "\n" +
                "ErrorMessage: " + getErrorMessage() + "\n" +
                "ErrorStackTrace: " + getErrorStackTrace() + "\n" +
                "ErrorFileName: " + getErrorFileName() + "\n" +
                "ErrorClassName: " + getErrorClassName() + "\n" +
                "ErrorMethodName: " + getErrorMethodName() + "\n" +
                "ErrorLineNumber: " + getErrorLineNumber() + "\n";
    }

    public static class Builder extends BaseEvent.Builder {
        private String mErrorExceptionClassName;
        private String mErrorMessage;
        private String mErrorStackTrace;
        private String mErrorFileName;
        private String mErrorClassName;
        private String mErrorMethodName;
        private Integer mErrorLineNumber;

        public Builder(String eventName, String eventCategory) {
            super(eventName, eventCategory);
        }

        public Builder withErrorExceptionClassName(String errorExceptionClassName) {
            mErrorExceptionClassName = errorExceptionClassName;
            return this;
        }

        public Builder withErrorMessage(String errorMessage) {
            mErrorMessage = errorMessage;
            return this;
        }

        public Builder withErrorStackTrace(String errorStackTrace) {
            mErrorStackTrace = errorStackTrace;
            return this;
        }

        public Builder withErrorFileName(String errorFileName) {
            mErrorFileName = errorFileName;
            return this;
        }

        public Builder withErrorClassName(String errorClassName) {
            mErrorClassName = errorClassName;
            return this;
        }

        public Builder withErrorMethodName(String errorMethodName) {
            mErrorMethodName = errorMethodName;
            return this;
        }

        public Builder withErrorLineNumber(Integer errorLineNumber) {
            mErrorLineNumber = errorLineNumber;
            return this;
        }

        public Builder withException(Exception exception) {
            mErrorExceptionClassName = exception.getClass().getName();
            mErrorMessage = exception.getMessage();

            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            mErrorStackTrace = stringWriter.toString();

            if (exception.getStackTrace().length > 0) {
                mErrorFileName = exception.getStackTrace()[0].getFileName();
                mErrorClassName = exception.getStackTrace()[0].getClassName();
                mErrorMethodName = exception.getStackTrace()[0].getMethodName();
                mErrorLineNumber = exception.getStackTrace()[0].getLineNumber();
            }
            return this;
        }

        @Override
        public ErrorEvent build() {
            return new ErrorEvent(this);
        }
    }
}
