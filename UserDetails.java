/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
//package dsfinalproject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author suresh
 */
public class UserDetails {
     //HashMap<String,String> groupIDAndIp;
    // HashMap<String,String> userIDAndIp;
     ArrayList<String> userId;
     ArrayList<String> groupID;
     ArrayList<String> groupLeaderIp;
     ArrayList<String> ip;
     ArrayList<String> sessionKey;
    UserDetails(){
        //groupIDAndIp=new HashMap<String,String>();
        //userIDAndIp=new HashMap<String,String>();
        groupID=new ArrayList<String>();
        groupLeaderIp=new ArrayList<String>();
        userId=new ArrayList<String>();
        ip=new ArrayList<String>();
        sessionKey=new ArrayList<String>();
    }
    public void insertEntry(String userID,String sessionK,String userIp){
        //userIDAndIp.put(userID, ip);
        userId.add(userID);
        ip.add(userIp);
        sessionKey.add(sessionK);
    }
    public void insertEntryGroup(String groupId,String gIp){
        groupID.add(groupId);
        groupLeaderIp.add(gIp);
    }
    public void updateLeader(String oldIp,String newIp){
        int index=groupLeaderIp.indexOf(oldIp);
        groupLeaderIp.set(index, newIp);
    }
    public String getSessionKey(String ipp){
        String key="";
        if(ip.contains(ipp)){
            int index=ip.indexOf(ipp);
            key=sessionKey.get(index);
        }
        else{
            key="";
        }
        return key;
    }
    public String getIp(String groupId){
        int index=groupID.indexOf(groupId);
        return groupLeaderIp.get(index);
    }
}
