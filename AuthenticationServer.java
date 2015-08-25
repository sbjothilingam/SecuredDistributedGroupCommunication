/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
//package dsfinalproject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 *
 * @author suresh
 */
public class AuthenticationServer extends Thread {

    /**
     * @param args the command line arguments
     */
    UserAndGroupDetails userAndGroup;
    String key = "authentication";
    static String userID="user";
    static int count=0;
    static int joinKey=0;
    static int comKey=0;
    public AuthenticationServer() {
        userAndGroup = new UserAndGroupDetails();
    }
    
    public String decryptionAuthenticationUser(String encryptedString) {
        String msg = "";
        try {
            Cipher c = Cipher.getInstance("AES");
            byte[] keyInBytes = key.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            c.init(Cipher.DECRYPT_MODE, k);
            byte[] decryptedMsg1 = new BASE64Decoder().decodeBuffer(encryptedString);
            byte[] decryptedMsg2 = c.doFinal(decryptedMsg1);
            String decryptedMsg = new String(decryptedMsg2);
            msg=decryptedMsg;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }
    //sending session key to the authenticated user encrypted with thw key
    public String encrptionAuthenticationUser(String userID,String sessionKey,String ipAdress) {
        String encryptedMsg = "";
        String msgToBeEncrypted=userID+","+sessionKey;
        try {
            byte[] keyInBytes = key.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            byte[] encryptedMsgInBytes = c.doFinal(msgToBeEncrypted.getBytes());
            encryptedMsg = new BASE64Encoder().encode(encryptedMsgInBytes);
            userAndGroup.insertEntry(userID, sessionKey, ipAdress);
            //printing the details of user added
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        //return encrypted message
        return encryptedMsg;
    }
    //decrypt the message received for the service grantor
    public String decryptionServiceGrantor(String encryptedString,String sKey) {
        String msg="";
        try{
          //  System.out.println("For Decryption "+encryptedString+" "+sKey);
            Cipher c = Cipher.getInstance("AES");
            byte[] keyInBytes = sKey.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            c.init(Cipher.DECRYPT_MODE, k);
            byte[] decryptedMsg1 = new BASE64Decoder().decodeBuffer(encryptedString);
            byte[] decryptedMsg2 = c.doFinal(decryptedMsg1);
            String decryptedMsg = new String(decryptedMsg2);
            msg=decryptedMsg;
        }
        catch(Exception e){
            e.printStackTrace();
        }//returns decrypted message
        return msg;
    }

    public String encryptionServiceGrantor(String sessionKey, String msg) {
        String encryptedMsg="";
        try{
            byte[] keyInBytes = sessionKey.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            byte[] encryptedMsgInBytes = c.doFinal(msg.getBytes());
            encryptedMsg = new BASE64Encoder().encode(encryptedMsgInBytes);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return encryptedMsg;
    }
    //send the list of available groups to the user
    public ArrayList<String> sendDeatils() {
        ArrayList<String> details=new ArrayList<String>();
        if(!userAndGroup.groupID.isEmpty()){
            for(int i=0;i<userAndGroup.groupID.size();i++){
                details.add(userAndGroup.groupID.get(i));
            }
        }
        else{
            details.add("null");
        }
        return details;
    }
    public void displayUserDetails(){
        System.out.println("USER DETAILS");
        System.out.println("---------------------------");
        for(int i=0;i<userAndGroup.userId.size();i++){
            System.out.println("| "+userAndGroup.userId.get(i) +"   |   "+userAndGroup.ip.get(i)+"   |");
            System.out.println("---------------------------");
        }
    }
    public void displayGroupDetails(){
        System.out.println("GROUP DETAILS");
        System.out.println("---------------------------");
        for(int i=0;i<userAndGroup.groupID.size();i++){
            System.out.println("| "+userAndGroup.groupID.get(i) +"   |   "+userAndGroup.groupLeaderIp.get(i)+"   |");
            System.out.println("---------------------------");
        }
    }
    public String sessionGrantorUpdateLeader(String encryptedMsg,String ip) {
        String status="";
        String sKey=userAndGroup.getSessionKey(ip);
        if(sKey.equals("")){
            status="reject";
            System.out.println("Rejection - No session key available");
        }
        else{
            String deadLeaderIp=decryptionServiceGrantor(encryptedMsg, sKey);
            System.out.println("Dead Leader "+deadLeaderIp);
            if(userAndGroup.groupLeaderIp.contains(deadLeaderIp)){
                String pingStatus=pingLeader(deadLeaderIp);
                if(pingStatus.equals("success")){
                    status="reject";
                    System.out.println("Rejection - Old Leader is still alive");
                }
                else{
                    status="success";
                }
            }
            else{
                status="reject";
                System.out.println("Rejection - Given Ip is not a leader");
            }
        }
        return status;
    }
    //to a join a group
    public String sessionGrantorJoinAGroup(String encryptedMsg,String ip) {
        String encryptedMsgToBeSent="";
        String sKey=userAndGroup.getSessionKey(ip);
        if(sKey.equals("")){
            encryptedMsgToBeSent="reject";
            System.out.println("Rejection - No session key available");
        }
        else{
            String[] joinRequest=decryptionServiceGrantor(encryptedMsg, sKey).split(",");
            if(userAndGroup.groupID.contains(joinRequest[1])){
                String leaderIp=userAndGroup.getIp(joinRequest[1]);
                String status=pingLeader(leaderIp);
                if(status.equals("success")){
                    joinKey+=1;
                    String jKey=joinRequest[0]+joinRequest[1]+joinKey;
                    String msg=jKey+","+leaderIp;
                    //msg to be encrypted which is to be sent to the destination
                    String msgDest=jKey+","+ip;
                    //getting the session key of the destination leader
                    String destSKey=userAndGroup.getSessionKey(leaderIp);
                    //encrypting the message with destion leader session key
                    String encryptedSendToDestGroup=encryptionServiceGrantor(destSKey, msgDest);
                    //sends the jKey to the destination with the requestors IP
                    sessionGrantorInformGroupJoin(leaderIp, encryptedSendToDestGroup);
                    encryptedMsgToBeSent=encryptionServiceGrantor(sKey, msg);
                }
                else{
                    encryptedMsgToBeSent="reject";
                    System.out.println("Rejection - Not able to establish connection");
                    deleteLeader(leaderIp);
                    deleteUser(leaderIp);
                }
            }
            else{
                encryptedMsgToBeSent="reject";
                System.out.println("Rejection - Not a leader");
            }
        }
        return encryptedMsgToBeSent;
    }
    //to inform the group with ckey and the groupId which wants to communicated with it
    public void sessionGrantorInformGroupCommunication(String groupIP, String encryptedMsg) {
        try {
            System.out.println("Informing group "+groupIP+" for communication");
            Socket sock=new Socket(groupIP, 1235);
            OutputStream os=sock.getOutputStream();
            DataOutputStream send=new DataOutputStream(os);
            send.writeUTF("receive communication");
            send.writeUTF(encryptedMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //to inform the group with jkey and the groupId which wants to join in its group
    public void sessionGrantorInformGroupJoin(String groupIP, String encryptedMsg){
        try{
            System.out.println("Informing group "+groupIP+" for join");
            Socket sock=new Socket(groupIP, 1235);
            OutputStream os=sock.getOutputStream();
            DataOutputStream send=new DataOutputStream(os);
            send.writeUTF("join request");
            send.writeUTF(encryptedMsg);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    //for communicating with a existing group
    public String sessionGrantorCommunicateWithAGroup(String encryptedMsg,String ip) {
        String encryptedMsgToBeSent="";
        String sKey=userAndGroup.getSessionKey(ip);
        if(sKey.equals("")){
            encryptedMsgToBeSent="reject";
            System.out.println("Rejection - No Session Key Available");
        }
        else{
            String[] communicateRequest=decryptionServiceGrantor(encryptedMsg, sKey).split(",");
            if(userAndGroup.groupID.contains(communicateRequest[1]) && userAndGroup.groupID.contains(communicateRequest[0])){
                String leaderIp=userAndGroup.getIp(communicateRequest[1]);
                String status=pingLeader(leaderIp);
                if(status.equals("success")){
                    comKey+=1;
                    String cKey=communicateRequest[0]+communicateRequest[1]+comKey;
                    String msg=cKey+","+leaderIp;
                    //msg to be encrypted which is to be sent to the destination(cKey with the ip of requestor group)
                    String msgDest=cKey+","+ip;
                    //getting the session key of the destination leader
                    String destSKey=userAndGroup.getSessionKey(leaderIp);
                    //encrypting the message with destion leader session key
                    String encryptedSendToDestGroup=encryptionServiceGrantor(destSKey, msgDest);
                    //sends the cKey to the destination with the requestors IP
                    sessionGrantorInformGroupCommunication(leaderIp, encryptedSendToDestGroup);
                    encryptedMsgToBeSent=encryptionServiceGrantor(sKey, msg);
                }
                else{
                    encryptedMsgToBeSent="reject";
                    System.out.println("Rejection - Not able to establish connection");
                    deleteLeader(leaderIp);
                    deleteUser(leaderIp);
                }
            }
            else{
                encryptedMsgToBeSent="reject";
                System.out.println("Rejection - Not a leader");
            }
        }
        return encryptedMsgToBeSent;
    }
    //for creating a new group
    public String sessionGrantorCreateGroup(String encryptedMsg,String ip) {
        String status="";
        try {
            //gets the session key for the received ip address from the table
            String sKey=userAndGroup.getSessionKey(ip);
            //if the session key is not available
            if(sKey.equals("")){
                status="reject";
                System.out.println("Rejection  - no session key available");
            }
            else{
                //callls the function with session key and encrypted message to get the group id
                String groupID=decryptionServiceGrantor(encryptedMsg, sKey);
                //if there is no group already exists in that name
                if(userAndGroup.groupID.contains(groupID)){
                    status="reject";
                    System.out.println("Rejection - group already exists");
                    }
                else {
                    userAndGroup.insertEntryGroup(groupID, ip);
                    status="success";
                } 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //return the status of group creation
        return status;
    }
    public String pingLeader(String leaderIp){
        System.out.println("Pinging "+leaderIp);
        String status="";
        try{
            Socket sock=new Socket(leaderIp,1235);
            status="success";
            }
        catch(Exception e){
            status="reject";
         }
        return status;
    }
    //delete leader from the table when it doesnt exists
    public void deleteLeader(String ip){
        if(userAndGroup.groupLeaderIp.contains(ip)){
            int index=userAndGroup.groupLeaderIp.indexOf(ip);
            userAndGroup.groupID.remove(index);
            userAndGroup.groupLeaderIp.remove(index);
        }
    }
    //delete the user from the storage
    public void deleteUser(String ip){
        if(userAndGroup.ip.contains(ip)){
            int indexU=userAndGroup.ip.indexOf(ip);
            userAndGroup.userId.remove(indexU);
            userAndGroup.ip.remove(indexU);
            userAndGroup.sessionKey.remove(indexU);
        }
    }
    @Override
    public void run() {
        ServerSocket server =null;
            try{
            System.out.println("Kerberos Like Authentication Server Started on " + InetAddress.getLocalHost().getHostAddress());
            server = new ServerSocket(1235);
            }
            catch(Exception e){
                e.printStackTrace();
            }
            String receiveIp = "";
            String request = "";
            while (true) {
                try {
                Socket accept = server.accept();
                InputStream is=accept.getInputStream();
                DataInputStream read=new DataInputStream(is);
                OutputStream os=accept.getOutputStream();
                DataOutputStream send=new DataOutputStream(os);
                receiveIp = accept.getInetAddress().getHostAddress();
                //request received from the user
                request = read.readUTF();
                if (request.equals("authenticateuser")) {
                    String encryptedMsg=read.readUTF();
                    String decryptedMsg=decryptionAuthenticationUser(encryptedMsg);
                    if(decryptedMsg.equals("authenticateme")){
                        if(!userAndGroup.ip.contains(receiveIp)){
                            count=count+1;
                            //creates a new session key for the user
                            String sessionKey=userID+count;
                            //encrypts the message with the created userid and session key for the user
                            String encryptedMsgToBeSent=encrptionAuthenticationUser(String.valueOf(count), sessionKey ,receiveIp);
                            send.writeUTF(encryptedMsgToBeSent);
                            displayUserDetails();
                        }
                        else{
                            send.writeUTF("reject");
                            System.out.println("Rejection - User already authenticated");
                        }
                    }
                    else{
                        send.writeUTF("reject");
                        System.out.println("Rejection - no valid keyword");
                    }
                } else if (request.equals("view")) {
                    ArrayList<String> details = sendDeatils();
                    for(int i=0;i<details.size();i++){
                        send.writeUTF(details.get(i));
                    }
                    send.writeUTF("#");
                } else if (request.equals("join")) {
                    String encryptedMsg = read.readUTF();
                    //encrypted msg with join key and leader Ip or reject message
                    String encryptedMsgToBeSent=sessionGrantorJoinAGroup(encryptedMsg,receiveIp);
                    send.writeUTF(encryptedMsgToBeSent);
                } else if (request.equals("communicate")) {
                    String encryptedMsg = read.readUTF();
                    //encrypts the message with cKey and leaderID
                    String encryptedMsgToBeSent=sessionGrantorCommunicateWithAGroup(encryptedMsg,receiveIp);
                    send.writeUTF(encryptedMsgToBeSent);
                } else if (request.equals("updateleader")) {
                    String encryptedMsg = read.readUTF();
                    String sKey=userAndGroup.getSessionKey(receiveIp);
                    String oldIp=decryptionServiceGrantor(encryptedMsg, sKey);
                    String status=sessionGrantorUpdateLeader(encryptedMsg,receiveIp);
                    if(status.equals("success")){
                        System.out.println("Group Leader changed from "+oldIp+" to "+receiveIp);
                        userAndGroup.updateLeader(oldIp, receiveIp);
                        send.writeUTF("success");
                        deleteUser(oldIp);
                    }
                    else{
                        send.writeUTF("reject");
                    }
                     displayUserDetails();
                     displayGroupDetails();
                } else if (request.equals("create")) {
                    String encryptedMsg = read.readUTF();
                    //encrypts msg with destgroupID and ip or reject
                    String sendMsg=sessionGrantorCreateGroup(encryptedMsg,receiveIp);
                    send.writeUTF(sendMsg);
                    displayGroupDetails();
                }
                } catch (Exception e) {
                    e.printStackTrace();
              }
        }
        
    }

    public static void main(String[] args) {
        // TODO code application logic here
        new AuthenticationServer().start();
    }
}
