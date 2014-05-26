package com.it.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;

public class Clusters implements Serializable {
    private static final long serialVersionUID = 3688785685416855060L;

    private Date bootupTime = new Date();
    private Map<String, MemberList> memberListMap = Maps.newHashMap();
    transient private static final MemberList EMPTY_MEMBERLIST = new MemberList();

    public Date getBootupTime() {
        return bootupTime;
    }

    public void add(String clusterName) {
        if (!memberListMap.containsKey(clusterName)) {
            memberListMap.put(clusterName, new MemberList());
        }
    }

    public void remove(String clusterName) {
        memberListMap.remove(clusterName);
    }

    public void add(String clusterName, Member member) {
        MemberList memberList = getMemberListIn(clusterName);
        if (memberList == EMPTY_MEMBERLIST) {
            add(clusterName);
            memberList = getMemberListIn(clusterName);
        }
        memberList.addMember(member);
    }

    public void remove(String clusterName, Member member) {
        MemberList memberList = getMemberListIn(clusterName);
        if (memberList.hasMembers()) {
            memberList.removeMember(member);
        }
    }

    public Set<String> getClusterNames() {
        return memberListMap.keySet();
    }

    public MemberList getMemberListIn(String cluster) {
        MemberList memberList = memberListMap.get(cluster);
        if (memberList == null) {
            return EMPTY_MEMBERLIST;
        }
        return memberList;
    }

    public Member nextRunningMember(String cluster) {
        MemberList memberList = getMemberListIn(cluster);
        return memberList.nextRunningMember();
    }

    public boolean setStatus(Member member, boolean isRunning) {
        for (Entry<String, MemberList> entry : memberListMap.entrySet()) {
            MemberList memberList = entry.getValue();
            if (memberList.setStatus(member.getHost(), member.getPort(),
                    isRunning)) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Clusters clusters) {
        if (memberListMap.size() != clusters.getClusterNames().size()) {
            return false;
        }

        for (String cluster : getClusterNames()) {
            if (getMemberListIn(cluster).getMembers().size() != clusters
                    .getMemberListIn(cluster).getMembers().size()) {
                return false;
            }

            Iterator<Member> iterator = getMemberListIn(cluster).getMembers()
                    .iterator();
            Iterator<Member> iterator2 = clusters.getMemberListIn(cluster)
                    .getMembers().iterator();
            while (iterator.hasNext()) {
                Member member = iterator.next();
                Member member2 = iterator2.next();
                if (!member.equals(member2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<String, MemberList> diff(Clusters categories) {
        Map<String, MemberList> result = Maps.newHashMap();
        for (String cluster : getClusterNames()) {
            MemberList memberList = getMemberListIn(cluster);
            if (memberList != EMPTY_MEMBERLIST) {
                result.put(cluster,
                        memberList.diff(categories.getMemberListIn(cluster)));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuffer result = new StringBuffer("\n* member list\n");
        for (String cluster : memberListMap.keySet()) {
            result.append("cluster: ").append(cluster).append(", members: ")
                    .append(memberListMap.get(cluster).toString())
                    .append("\n");
        }

        return result.toString();
    }
}