package com.it.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.it.exception.InvalidMemberException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class MemberList implements Serializable {
  private static final long serialVersionUID = -5384674645915944849L;

  private static final Logger logger = LoggerFactory.getLogger(MemberList.class);

  private List<Member> members = Lists.newArrayList();
  transient private int index = 0;

  public MemberList() {}

  public boolean hasMembers() {
    return members.size() > 0;
  }

  public int size() {
    return members.size();
  }

  public Member findMe() {
    for (Member member : members) {
      if (member.isMe()) {
        return member;
      }
    }
    return null;
  }

  public Member getMember(int id) {
    for (Member member : members) {
      if (member.getId() == id) {
        return member;
      }
    }
    return null;
  }

  public List<Member> getMembers() {
    return members;
  }

  @JsonIgnore
  public List<Member> getRunningMembers() {
    List<Member> runningMembers = Lists.newArrayList();
    for (Member member : members) {
      if (member.isRunning()) {
        runningMembers.add(member);
      }
    }
    return runningMembers;
  }

  public Member nextRunningMember() {
    Member result = null;
    for (int i = 0; i < members.size(); i++) {
      Member member = members.get(next());
      if (member.isRunning() && !member.isMe()) {
        result = member;
        break;
      }
    }
    return result;
  }

  public int next() {
    index = (index + 1) % members.size();
    return index;
  }

  public boolean setStatus(Member member, Status status) {
    member.setStatus(status);
    return true;
  }

  public boolean setStatus(String host, int port, Status status) {
    Member member = findMemberByDataPort(host, port);
    if (member == null) {
      return false;
    }
    member.setStatus(status);

    return true;
  }

  public boolean contains(Member member) {
    if (findMemberByDataPort(member.getHost(), member.getDataPort()) == null) {
      return false;
    }
    return true;
  }

  public Member findMemberByDataPort(String host, int dataPort) {
    for (Member member : members) {
      if (member.equalsByDataPort(host, dataPort)) {
        return member;
      }
    }
    return null;
  }

  public Member findMemberByControlPort(String host, int controlPort) {
    for (Member member : members) {
      if (member.equalsByDataPort(host, controlPort)) {
        return member;
      }
    }
    return null;
  }

  public MemberList diff(MemberList memberList) {
    MemberList result = new MemberList();
    for (Member member : getMembers()) {
      if (memberList == null || !memberList.contains(member)) {
        result.addMember(member);
      }
    }
    return result;
  }

  // TODO: change member2
  public boolean isDuplicatedId(Member member2) {
    for (Member member : members) {
      if (member.getId() == member2.getId()) {
        return true;
      }
    }
    return false;
  }

  public void addMember(Member member) {
    if (isDuplicatedId(member)) {
      logger.error("The id({}) is duplicated.", member.getId());
      throw new InvalidMemberException("The id is duplicated.");
    }
    members.add(member);
  }

  public void removeMember(Member member) {
    members.remove(member);
  }

  @Override
  public String toString() {
    return members.toString();
  }
}
