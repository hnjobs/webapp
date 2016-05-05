package com.emilburzo.hnjobs.shared.rpc;

import java.io.Serializable;

public class JobRPC implements Serializable {

    public String id;
    public String author;
    public Long timestamp;
    public String bodyHtml;
    public Float score;

}
