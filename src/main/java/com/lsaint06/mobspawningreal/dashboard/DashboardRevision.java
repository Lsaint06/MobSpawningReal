package com.lsaint06.mobspawningreal.dashboard;

/** Observing state and publishing it are separate: reads cannot consume a broadcast. */
public final class DashboardRevision {
    private long rules=-1, state=-1, current, published=-1;
    public boolean observe(long ruleRevision,long stateRevision) {
        if(rules==ruleRevision&&state==stateRevision)return false;
        rules=ruleRevision;state=stateRevision;current++;return true;
    }
    public long current(){return current;}
    public boolean needsBroadcast(){return published!=current;}
    public void broadcasted(){published=current;}
}
