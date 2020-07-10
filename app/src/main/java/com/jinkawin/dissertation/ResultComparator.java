package com.jinkawin.dissertation;

import java.util.Comparator;

public class ResultComparator implements Comparator<Result> {
    @Override
    public int compare(Result o1, Result o2) {
        return o1.getIndex() - o2.getIndex();
    }
}
