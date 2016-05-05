package com.emilburzo.hnjobs.shared.rpc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchResultRPC implements Serializable {

    public List<JobRPC> jobs = new ArrayList<>();

    public long duration;
}
