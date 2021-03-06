package com.jinkawin.dissertation;

public enum ProcessStatus {
    PROCESSING(Constants.PROCESSING),
    SUCCESS(Constants.SUCCESS),
    FAIL(Constants.FAIL),
    FINISH(Constants.FINISH);

    private final int value;

    ProcessStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static ProcessStatus intToEnum(int value) {
        switch(value) {
            case Constants.PROCESSING:
                return ProcessStatus.PROCESSING;
            case Constants.SUCCESS:
                return ProcessStatus.SUCCESS;
            case Constants.FAIL:
                return ProcessStatus.FAIL;
            case Constants.FINISH:
                return ProcessStatus.FINISH;
        }
        return null;
    }

    private static class Constants {
        public static final int PROCESSING = 0;
        public static final int SUCCESS = 1;
        public static final int FAIL = 2;
        public static final int FINISH = 3;
    }
}
