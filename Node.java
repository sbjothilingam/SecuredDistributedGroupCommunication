import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
/**
 *
  * @author Suresh Babu Jothilingam
 
 */
public class Node {
    public static void main(String args[]) throws IOException{
        Scanner s1 = new Scanner(System.in);
        Scanner s3 = new Scanner(System.in);
        Info Info = new Info();
        System.out.println(InetAddress.getLocalHost());
        System.out.println("enter the authenticator ip");
        Info.AuthenticatorIp = s3.nextLine();
        int port = Info.port;
        ServerHead server = new ServerHead(port);
        KeepPingingLeader ping = new KeepPingingLeader();
        server.start();
        
        while(true){
            System.out.println();
            System.out.println("Enter the command: 1.view groups 2.authenticate for session key 3.create a group 4.join a group");
            System.out.println("5.send msg to a group");
            int userinput = s1.nextInt();
            if(userinput == 1){
                //contacting authenticator for view
                Socket socket = new Socket(Info.AuthenticatorIp,port);
                InputStream is=socket.getInputStream();
                DataInputStream dis=new DataInputStream(is);
                OutputStream os=socket.getOutputStream();
                DataOutputStream dos=new DataOutputStream(os);
                dos.writeUTF("view");
                String input = dis.readUTF();
                while(!input.equals("#")){
                    Info.ListOfGroupId.add(input);
                    System.out.println("groupid "+input);
                    input = dis.readUTF();
                }
                //socket.close();
                //System.out.println("list of group ids stored");
            }
            else if(userinput == 2){
                //authenticating to get session key
                Socket socket = new Socket(Info.AuthenticatorIp,port);
                InputStream is=socket.getInputStream();
                DataInputStream dis=new DataInputStream(is);
                OutputStream os=socket.getOutputStream();
                DataOutputStream dos=new DataOutputStream(os);
                dos.writeUTF("authenticateuser");
                //enrypting the message
                String encryptedMsg = encrypt("authenticateme","authentication");
                // sending encrypted msg to authenticator
                dos.writeUTF(encryptedMsg);
                // receiving reply
                String encryptedString = dis.readUTF();
                //decrypting the reply to get id and session key
                String decryptedMsg = decrypt(encryptedString,"authentication");
                String split[] = decryptedMsg.split(",");
                // storing id and session key
                Info.Id = split[0];
                Info.SessionKey = split[1];
                System.out.println("user id and session key obtained");
                //socket.close();
                
            }else if(userinput==3){
                //contacting authenticator to create a group
                String groupId;
                Random r = new Random();
                // creating random group id not in already present group ids
                while(true){
                    int t = r.nextInt()%100;
                    if(t<0){
                        t*=-1;
                    }
                    groupId = t+"";
                    if(!Info.ListOfGroupId.contains(t)){
                        break;
                    }
                }
                Socket socket = new Socket(Info.AuthenticatorIp,port);
                InputStream is=socket.getInputStream();
                DataInputStream dis=new DataInputStream(is);
                OutputStream os=socket.getOutputStream();
                DataOutputStream dos=new DataOutputStream(os);
                dos.writeUTF("create");
                //enrypting the message with session key
                String encryptedMsg = encrypt(groupId,Info.SessionKey);
                System.out.println("groupId "+groupId);
                // sending encrypted msg to authenticator
                dos.writeUTF(encryptedMsg);
                String reply = dis.readUTF();
                if(reply.equals("success")){
                    System.out.println("group creation successful, group id: "+groupId);
                    Info.MainLeader = "self";
                    Info.MainLeaderGroup = groupId;
                }
                else if(reply.equals("reject")){
                    System.out.println("create group failed");
                }else{
                    System.out.println("server sent "+reply+" !!");
                }
            }else if(userinput == 4){
                // joining a group
                Socket socket = new Socket(Info.AuthenticatorIp,port);
                InputStream is=socket.getInputStream();
                DataInputStream dis=new DataInputStream(is);
                OutputStream os=socket.getOutputStream();
                DataOutputStream dos=new DataOutputStream(os);
                dos.writeUTF("join");
                //enrypting the message with session key
                System.out.println("enter the group id");
                String groupId = s1.nextInt()+"";
                String encryptedMsg = encrypt(Info.Id+","+groupId,Info.SessionKey);   
                try{
                System.out.println("groupId "+groupId);
                // sending encrypted msg to authenticator
                dos.writeUTF(encryptedMsg);
                String reply = dis.readUTF();
                if(reply.equals("reject")){
                    System.out.println("join group failed");
                }
                else{   
                    Scanner s2 = new Scanner(System.in);
                    //decrypting the reply to join key and leader Ip
                    String decryptedMsg = decrypt(reply,Info.SessionKey);
                    String split[] = decryptedMsg.split(",");
                    // storing 
                    Info.JoinKey = split[0];
                    // setting leader ip
                    Info.MainLeader = split[1];
                    System.out.println("join key and leader ip obtained "+Info.JoinKey+" "+Info.MainLeader);
                    //socket.close();
                    System.out.println("do you want to proceed with the join ?");
                    if(s2.nextLine().equals("yes")){
                        // setting the group id of group joined
                        Info.MainLeaderGroup = groupId;

                        //contacting leader to join the group
                       // Socket socket1 = new Socket(Info.MainLeader,port);
                        String MainLeader = Info.MainLeader;
                        Socket socket1 = new Socket(MainLeader,port);
                        InputStream is1=socket1.getInputStream();
                        DataInputStream dis1=new DataInputStream(is1);
                        OutputStream os1=socket1.getOutputStream();
                        DataOutputStream dos1=new DataOutputStream(os1);
                        dos1.writeUTF("join");
                        dos1.writeUTF(Info.Id);
                        encryptedMsg = encrypt("join me",Info.JoinKey);
                        dos1.writeUTF(encryptedMsg);
                        int size = dis1.readInt();
                        for(int i=0;i<size;i++){
                            Info.GroupNodesList.add(dis1.readUTF());
                        }
                        for(int i=0;i<size;i++){
                            Info.GroupNodesListId.add(dis1.readUTF());
                        }
                        System.out.println("successfully joined in group "+groupId);
                        Info.MainLeader=MainLeader;
                        new Thread(ping).start();
                        socket1.close();
                    }
                }
                }catch(Exception e)
                {
                    e.printStackTrace();
                }
            }else if(userinput==5){
                Scanner s2 = new Scanner(System.in);
                System.out.println("enter the message");
                String message = s2.nextLine();
                System.out.println("enter the groupId");
                String groupid = s2.nextLine();
                // if the current node is the leader 
                if(Info.MainLeader.equals("self")){
                    if(Info.MainLeaderGroup.equals(groupid)){
                        System.out.println("message received for leader : "+message);
                        for(int i=0;i<Info.ListOfGroupId.size();i++){
                            Socket socket = new Socket(Info.ListOfGroupId.get(i),port);
                            InputStream is=socket.getInputStream();
                            DataInputStream dis=new DataInputStream(is);
                            OutputStream os=socket.getOutputStream();
                            DataOutputStream dos=new DataOutputStream(os);
                            dos.writeUTF("incoming message");
                            dos.writeUTF(message);
                            //socket.close();
                        }   
                    }else{
                        System.out.println("message received for leader : "+message);
                        //contact auhenticator to get the comm key and ip
                        Socket socket = new Socket(Info.AuthenticatorIp,port);
                        InputStream is=socket.getInputStream();
                        DataInputStream dis=new DataInputStream(is);
                        OutputStream os=socket.getOutputStream();
                        DataOutputStream dos=new DataOutputStream(os);
                        dos.writeUTF("communicate");
                        //enrypting the message with session key
                        String encryptedMsg = encrypt(Info.MainLeaderGroup+","+groupid,Info.SessionKey);
                        try{
                        System.out.println("groupId "+groupid);
                        // sending encrypted msg to authenticator
                        dos.writeUTF(encryptedMsg);
                        String reply = dis.readUTF();
                        if(reply.equals("reject")){
                            System.out.println("comm failed");
                        }
                        else{
                            //decrypting the reply to get comm key and comm leader Ip
                            String decryptedMsg = decrypt(reply,Info.SessionKey);
                            String split[] = decryptedMsg.split(",");
                            // storing 
                            Info.communicationKey = split[0];
                            // setting comm ip
                            Info.communicationIp = split[1];
                            System.out.println("communication key and communication ip obtained "+Info.communicationKey+" "+Info.communicationIp);
                           // socket.close();
                            
                            //contacting that leader to send the encrypted message
                            sendMsgToLeader(message,Info.communicationKey,Info.communicationIp);
                        }
                        }
                        catch(Exception e)
                        {
                            System.out.println(e);
                        }
                    }
                }else{ // contact leader
                    Socket socket = new Socket(Info.MainLeader,port);
                    InputStream is=socket.getInputStream();
                    DataInputStream dis=new DataInputStream(is);
                    OutputStream os=socket.getOutputStream();
                    DataOutputStream dos=new DataOutputStream(os);
                    dos.writeUTF("send message to a group");
                    dos.writeUTF(message);
                    dos.writeUTF(groupid);
                    //socket.close();
                }
            }
        }
    }
    static String encrypt(String message,String key){
        String encryptedMsg = "";
        try {
            String msgToBeEncrypted = message;
            byte[] keyInBytes = key.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            byte[] encryptedMsgInBytes = c.doFinal(msgToBeEncrypted.getBytes());
            encryptedMsg = new BASE64Encoder().encode(encryptedMsgInBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return encryptedMsg;
    }
    static String decrypt(String message,String key){
        String decryptedMsg = "";
        try {
            byte[] keyInBytes = key.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, k);
            byte[] decryptedMsg1 = new BASE64Decoder().decodeBuffer(message);
            byte[] decryptedMsg2 = c.doFinal(decryptedMsg1);
            decryptedMsg = new String(decryptedMsg2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return decryptedMsg;
    }
    static void sendMsgToLeader(String message,String communicationKey,String LeaderIp){
        try {
            Info Info = new Info();
            Socket socket = new Socket(LeaderIp,Info.port);
            OutputStream os=socket.getOutputStream();
            DataOutputStream dos=new DataOutputStream(os);
            dos.writeUTF("receive msg from another group"); 
            String encryptedMsg = "";
            String msgToBeEncrypted = message;
            byte[] keyInBytes = communicationKey.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            byte[] encryptedMsgInBytes = c.doFinal(msgToBeEncrypted.getBytes());
            encryptedMsg = new BASE64Encoder().encode(encryptedMsgInBytes);
            dos.writeUTF(encryptedMsg);
            System.out.println("sent encrypted msg to Leader: "+LeaderIp);
            //socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
}
class ServerHead extends Thread{ // creating multiple servers
    int port;
    Socket server;
    ServerSocket listener;
    ServerHead(int port) throws IOException {
        this.port = port;
        listener = new ServerSocket(port);
    }
    public void run(){
        while(true){
            try {
                server = listener.accept();
            } catch (IOException ex) {
                Logger.getLogger(ServerHead.class.getName()).log(Level.SEVERE, null, ex);
            }
        Server conn= new Server(server);
        Thread t = new Thread(conn);
        t.start();
      }
    }
}
class Server implements Runnable{ // server
    static void sendUpdatedGroupList() throws IOException{
        Info Info = new Info();
        for(int j=0;j<Info.GroupNodesList.size();j++){
            Socket socket = new Socket(Info.GroupNodesList.get(j),Info.port);
            OutputStream os=socket.getOutputStream();
            DataOutputStream dos=new DataOutputStream(os);
            dos.writeUTF("update group list");
            dos.writeInt(Info.GroupNodesList.size());
            for(int i=0;i<Info.GroupNodesList.size();i++){
                dos.writeUTF(Info.GroupNodesList.get(i));
            }
            for(int i=0;i<Info.GroupNodesList.size();i++){
                dos.writeUTF(Info.GroupNodesListId.get(i));
            }
            //socket.close();
        }
    }
    static String encrypt(String message,String key){
        String encryptedMsg = "";
        try {
            String msgToBeEncrypted = message;
            byte[] keyInBytes = key.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            byte[] encryptedMsgInBytes = c.doFinal(msgToBeEncrypted.getBytes());
            encryptedMsg = new BASE64Encoder().encode(encryptedMsgInBytes);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return encryptedMsg;
    }
    static String decrypt(String message,String key){
        String decryptedMsg = "";
        try {
            byte[] keyInBytes = key.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, k);
            byte[] decryptedMsg1 = new BASE64Decoder().decodeBuffer(message);
            byte[] decryptedMsg2 = c.doFinal(decryptedMsg1);
            decryptedMsg = new String(decryptedMsg2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return decryptedMsg;
    }
    static void sendMsgToLeader(String message,String communicationKey,String LeaderIp){
        try {
            Info Info = new Info();
            Socket socket = new Socket(LeaderIp,Info.port);
            OutputStream os=socket.getOutputStream();
            DataOutputStream dos=new DataOutputStream(os);
            dos.writeUTF("receive msg from another group"); 
            String encryptedMsg = "";
            String msgToBeEncrypted = message;
            byte[] keyInBytes = communicationKey.getBytes();
            keyInBytes = Arrays.copyOf(keyInBytes, 16);
            Key k = new SecretKeySpec(keyInBytes, "AES");
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            byte[] encryptedMsgInBytes = c.doFinal(msgToBeEncrypted.getBytes());
            encryptedMsg = new BASE64Encoder().encode(encryptedMsgInBytes);
            dos.writeUTF(encryptedMsg);
            System.out.println("sent encrypted msg to leader: "+LeaderIp);
            //socket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    static void sendMsgToAll(String msg) throws IOException{
        Info Info = new Info();
        for(int j=0;j<Info.GroupNodesList.size();j++){
            Socket socket = new Socket(Info.GroupNodesList.get(j),Info.port);
            OutputStream os=socket.getOutputStream();
            DataOutputStream dos=new DataOutputStream(os);
            dos.writeUTF("incoming message");
            dos.writeUTF(msg);
            //socket.close();
        }
    }
    public void attemptToElectNewLeader()
    {
        
        boolean atLeastOneReplied;
        if(!Info.isLeaderElectionHappening)
        {
            Info.isLeaderElectionHappening = true;
            atLeastOneReplied = false;
            String LeaderToBeRemoved = Info.MainLeader; //temporarily store leader ip
            
            for(int i=0; i<Info.GroupNodesList.size(); i++) // remove leader
            {
                if(Info.GroupNodesList.get(i).equalsIgnoreCase(LeaderToBeRemoved))
                {
                    Info.GroupNodesList.remove(i);
                    Info.GroupNodesListId.remove(i);
                    break;
                }
            }
            
            //System.out.println("size "+ Info.GroupNodesList.size()+" "+Info.GroupNodesListId.size());
            for(int i=0; i<Info.GroupNodesList.size(); i++)
            {
                //System.out.println(Info.Id+" "+Info.GroupNodesListId.get(i));
                String neigh = Info.GroupNodesList.get(i);
                int id = Integer.parseInt(Info.GroupNodesListId.get(i));
                //if(neigh.equalsIgnoreCase(Info.ownIp))
                //    continue;
                try {
                if(neigh.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress().toString()))
                    continue;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                if(id > Integer.parseInt(Info.Id))
                {
                    //System.out.println("Id: "+id);
                    try 
                    {
                        Socket socket = new Socket();
                        InetSocketAddress sockaddr = new InetSocketAddress(neigh, Info.port);
                        socket.connect(sockaddr, 1000);
                        OutputStream os=socket.getOutputStream();
                        DataOutputStream dos=new DataOutputStream(os);
                        dos.writeUTF("Elect");
                        System.out.println("elect sent to id "+id+" ip:"+neigh);
                        atLeastOneReplied = true;
                        socket.close();
                    }  
                    catch(Exception exx)
                    {
                        System.out.println("Exception: "+exx.getMessage());
                        //Info.GroupNodesList.remove(i);
                        //Info.GroupNodesListId.remove(i);
                        //i--;
                    }
                }
            }
            if(!atLeastOneReplied)  // multicast itself as the leader
            {
                try {
                    Info.isLeaderElectionHappening = false;
                    Info.MainLeader = InetAddress.getLocalHost().getHostAddress().toString();
                    System.out.println("Main leader updated "+Info.MainLeader);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                
                //System.out.println("size "+ Info.GroupNodesList.size()+" "+Info.GroupNodesListId.size());
                for(int i=0; i<Info.GroupNodesList.size(); i++)
                {
                    String neigh = Info.GroupNodesList.get(i);
                    try {
                        if(neigh.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress().toString()))
                            continue;
                        if(neigh.equalsIgnoreCase(Info.MainLeader))
                            continue;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try
                    {
                        Socket socket = new Socket();
                        //System.out.println("New leader updation about to be sent to "+neigh);
                        InetSocketAddress sockaddr = new InetSocketAddress(neigh, Info.port);
                        socket.connect(sockaddr, 1000);
                        OutputStream os=socket.getOutputStream();
                        DataOutputStream doss=new DataOutputStream(os);
                        doss.writeUTF("NewLeader");
                        System.out.println("New leader updation sent to "+neigh);
                        socket.close();
                    }
                    catch(Exception exx)
                    {
                        System.out.println("Exception: "+exx.getMessage());
                        //Info.GroupNodesList.remove(i);
                        //Info.GroupNodesListId.remove(i);
                        //i--;
                    }
                }
            
                //Send to kerberos
                try 
                {
                    Socket socket=new Socket();
                    InetSocketAddress sockaddr = new InetSocketAddress(Info.AuthenticatorIp, Info.port);
                    socket.connect(sockaddr, 1000);
                    OutputStream os=socket.getOutputStream();
                    DataOutputStream doss=new DataOutputStream(os);
                    doss.writeUTF("updateleader");
                    String updateRequest=encrypt(LeaderToBeRemoved,Info.SessionKey);
                    doss.writeUTF(updateRequest);
                    socket.close();
                } 
                catch (IOException ex) 
                {
                    Logger.getLogger(KeepPingingLeader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    private Socket socket;
    Server(Socket server){
        socket = server;
    }
    public void run(){
        InputStream is=null;
        Info Info = new Info();
        try {
            is = socket.getInputStream();
            DataInputStream dis=new DataInputStream(is);
            OutputStream os=socket.getOutputStream();
            DataOutputStream dos=new DataOutputStream(os);
            String input = dis.readUTF();
            // switch case
            if(input.equals("join request")){ // from authenticator
                String reply = dis.readUTF();
                String decryptedMsg = decrypt(reply,Info.SessionKey);
                String split[] = decryptedMsg.split(",");
                Info.LeadersJoinKey = split[0];
                System.out.println("join key received "+Info.LeadersJoinKey);
                //socket.close();
            }
            if(input.equals("join")){
                String id = dis.readUTF();
                String reply = dis.readUTF();
                //decrypting the reply to join key and leader Ip
                String decryptedMsg = decrypt(reply,Info.LeadersJoinKey);
                if(decryptedMsg.equals("join me")){
                    //sending list of ips in group
                    dos.writeInt(Info.GroupNodesList.size());
                    for(int i=0;i<Info.GroupNodesList.size();i++){
                        dos.writeUTF(Info.GroupNodesList.get(i));
                    }
                    for(int i=0;i<Info.GroupNodesList.size();i++){
                        dos.writeUTF(Info.GroupNodesListId.get(i));
                    }
                    Info.GroupNodesList.add(socket.getInetAddress().getHostAddress());
                    Info.GroupNodesListId.add(id);
                System.out.println("node with ip "+socket.getInetAddress().getHostAddress()+" added to group");
                }
                sendUpdatedGroupList();
                
            }else if(input.equals("update group list")){
                int size = dis.readInt();
                Info.GroupNodesList.clear();
                Info.GroupNodesListId.clear();
                for(int i=0;i<size;i++){
                    Info.GroupNodesList.add(dis.readUTF());
                }
                for(int i=0;i<size;i++){
                    Info.GroupNodesListId.add(dis.readUTF());
                }
            }else if(input.equals("incoming message")){
                System.out.println("message received "+dis.readUTF());
            }else if(input.equals("send message to a group")){
                String message = dis.readUTF();
                String groupid = dis.readUTF();
                System.out.println("message received: "+message);
                if(Info.MainLeaderGroup.equals(groupid)){
                    for(int i=0;i<Info.GroupNodesList.size();i++){
                        Socket socket1 = new Socket(Info.GroupNodesList.get(i),Info.port);
                        OutputStream os1=socket1.getOutputStream();
                        DataOutputStream dos1=new DataOutputStream(os1);
                        dos1.writeUTF("incoming message");
                        dos1.writeUTF(message);
                        socket1.close();
                    }   
                }else{ //contact authenticator to get comm key and ip
                        Socket socket1 = new Socket(Info.AuthenticatorIp,Info.port);
                        InputStream is1=socket1.getInputStream();
                        DataInputStream dis1=new DataInputStream(is1);
                        OutputStream os1=socket1.getOutputStream();
                        DataOutputStream dos1=new DataOutputStream(os1);
                        dos1.writeUTF("communicate");
                        //enrypting the message with session key
                        String encryptedMsg = encrypt(Info.MainLeaderGroup+","+groupid,Info.SessionKey);
                        try{
                        System.out.println("groupId "+groupid);
                        // sending encrypted msg to authenticator
                        dos1.writeUTF(encryptedMsg);
                        String reply = dis1.readUTF();
                        if(reply.equals("reject")){
                            System.out.println("comm failed");
                        }
                        else{
                            //decrypting the reply for join key and leader Ip
                            String decryptedMsg = decrypt(reply,Info.SessionKey);
                            String split[] = decryptedMsg.split(",");
                            // storing 
                            Info.communicationKey = split[0];
                            // setting comm ip
                            Info.communicationIp = split[1];
                            System.out.println("communication key and communication ip obtained "+Info.communicationKey+" "+Info.communicationIp);
                            socket1.close();
                            sendMsgToLeader(message,Info.communicationKey,Info.communicationIp);
                        }
                }catch(Exception e){
                    e.printStackTrace();
                }
                }
            }else if(input.equals("receive communication")){ //receive communication from authenticator
                String reply = dis.readUTF();
                byte[] keyInBytes = Info.SessionKey.getBytes();
                keyInBytes = Arrays.copyOf(keyInBytes, 16);
                Key k = new SecretKeySpec(keyInBytes, "AES");
                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, k);
                byte[] decryptedMsg1 = new BASE64Decoder().decodeBuffer(reply);
                byte[] decryptedMsg2 = c.doFinal(decryptedMsg1);
                String decryptedMsg = new String(decryptedMsg2);
                String split[] = decryptedMsg.split(",");
                Info.LeadersCommKey = split[0];
                System.out.println("comm key received "+Info.LeadersCommKey);
                
            }else if(input.equals("receive msg from another group")){
                String message = dis.readUTF();
                //socket.close();
                 byte[] keyInBytes = Info.LeadersCommKey.getBytes();
                keyInBytes = Arrays.copyOf(keyInBytes, 16);
                Key k = new SecretKeySpec(keyInBytes, "AES");
                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE, k);
                byte[] decryptedMsg1 = new BASE64Decoder().decodeBuffer(message);
                byte[] decryptedMsg2 = c.doFinal(decryptedMsg1);
                String decryptedMsg = new String(decryptedMsg2);
                System.out.println("received message: "+decryptedMsg);
                // send msg to all group members
                sendMsgToAll(decryptedMsg);
            }
            else if(input.equalsIgnoreCase("Elect")){
                attemptToElectNewLeader();
                System.out.println("elect received from "+socket.getInetAddress().getHostAddress());
            }
            else if(input.equalsIgnoreCase("NewLeader"))
            {
                Info.MainLeader = socket.getInetAddress().getHostAddress();
                Info.isLeaderElectionHappening = false;
                System.out.println("New leader is "+Info.MainLeader);
            }
            else if(input.equals("IsAlive")){
                //System.out.println("is alive received");
            }
            input = "";
            socket.close();
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
}
class KeepPingingLeader implements Runnable
{

    public void electLeader() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException
    {
        //initiate leader election
        Info.isLeaderElectionHappening = true;
        SocketAddress sockaddr = null;
        
        boolean atLeastOneReplied;
        
        String LeaderToBeRemoved = Info.MainLeader; //temporarily store leader ip
        Info.isLeaderElectionHappening = true;
        atLeastOneReplied = false;
        for(int i=0; i<Info.GroupNodesList.size(); i++) // remove leader
        {
            if(Info.GroupNodesList.get(i).equalsIgnoreCase(LeaderToBeRemoved))
            {
                Info.GroupNodesList.remove(i);
                Info.GroupNodesListId.remove(i);
                break;
            }
        }
        //System.out.println("size "+ Info.GroupNodesList.size()+" "+Info.GroupNodesListId.size());
        for(int i=0; i<Info.GroupNodesList.size(); i++)
        {
            //System.out.println(Info.Id+" "+Info.GroupNodesListId.get(i));
            String neigh = Info.GroupNodesList.get(i);
            int id = Integer.parseInt(Info.GroupNodesListId.get(i));
             //if(neigh.equalsIgnoreCase(Info.ownIp))
                //    continue;
            try {
                if(neigh.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress().toString()))
                    continue;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if(id > Integer.parseInt(Info.Id))
            {
                //System.out.println("Id "+id);
                try 
                {
                    Socket socket = new Socket();
                    sockaddr = new InetSocketAddress(neigh, Info.port);
                    socket.connect(sockaddr, 1000);
                    OutputStream os=socket.getOutputStream();
                    DataOutputStream dos=new DataOutputStream(os);
                    dos.writeUTF("Elect");
                    System.out.println("elect sent to id "+id+" ip:"+neigh);
                    atLeastOneReplied = true;
                    socket.close();
                 } 
                 catch(Exception exx)
                 {
                    System.out.println("Exception: "+exx.getMessage());
                    //Info.GroupNodesList.remove(i);
                    //Info.GroupNodesListId.remove(i);
                    //i--;
                 }
            }
        }
        if(!atLeastOneReplied)  // multicast itself as the leader
        {
            try {
                Info.isLeaderElectionHappening = false;
                Info.MainLeader = InetAddress.getLocalHost().getHostAddress().toString();
                
                System.out.println("Main leader updated "+Info.MainLeader);
            } catch (Exception ex) {
                Logger.getLogger(KeepPingingLeader.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //System.out.println("size "+ Info.GroupNodesList.size()+" "+Info.GroupNodesListId.size());
            for(int i=0; i<Info.GroupNodesList.size(); i++)
            {
                String neigh = Info.GroupNodesList.get(i);
                try {
                if(neigh.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress().toString()))
                    continue;
                if(neigh.equalsIgnoreCase(Info.MainLeader))
                    continue;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try
                {
                    Socket socket = new Socket();
                    //System.out.println("New leader updation about to be sent to "+neigh);
                    sockaddr = new InetSocketAddress(neigh, Info.port);
                    socket.connect(sockaddr, 1000);
                    OutputStream os=socket.getOutputStream();
                    DataOutputStream doss=new DataOutputStream(os);
                    doss.writeUTF("NewLeader");
                    System.out.println("New leader updation sent to "+neigh);
                    socket.close();
                }
                catch(Exception exx)
                {
                    System.out.println("Exception: "+exx.getMessage());
                    //Info.GroupNodesList.remove(i);
                    //Info.GroupNodesListId.remove(i);
                    //i--;
                } 
             }
             
            //Send to kerberos
            try {
                 Socket socket=new Socket();
                    sockaddr = new InetSocketAddress(Info.AuthenticatorIp, Info.port);
                    socket.connect(sockaddr, 1000);
                    OutputStream os=socket.getOutputStream();
                    DataOutputStream doss=new DataOutputStream(os);
                    doss.writeUTF("updateleader");
                    String msgToBeEncrypted = LeaderToBeRemoved;
                    byte[] keyInBytes = Info.SessionKey.getBytes();
                    keyInBytes = Arrays.copyOf(keyInBytes, 16);
                    Key k = new SecretKeySpec(keyInBytes, "AES");
                    Cipher c = Cipher.getInstance("AES");
                    c.init(Cipher.ENCRYPT_MODE, k);
                    byte[] encryptedMsgInBytes = c.doFinal(msgToBeEncrypted.getBytes());
                    String updateRequest = new BASE64Encoder().encode(encryptedMsgInBytes);
                    doss.writeUTF(updateRequest);
                    socket.close();
            } catch (IOException ex) {
                Logger.getLogger(KeepPingingLeader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    @Override
    public void run() 
    {
        while(true)
        {
            SocketAddress sockaddr = null;
            Socket socket = new Socket();
            boolean atLeastOneReplied;
            try {
                
                if(!Info.isLeaderElectionHappening && !(Info.MainLeader.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress().toString()))) 
                {
                    //System.out.println("enter pinging");
                    try
                    {
                        sockaddr = new InetSocketAddress(Info.MainLeader, Info.port);
                        socket.connect(sockaddr, 1000);
                        OutputStream os=socket.getOutputStream();
                        DataOutputStream dos=new DataOutputStream(os);
                        dos.writeUTF("IsAlive");
                        //System.out.println("sent is alive");
                        socket.close();
                        Thread.sleep(1000);
                    }
                    catch (java.net.ConnectException ex)
                    {
                        Info.isLeaderElectionHappening = true;
                        try {
                            electLeader();
                            //ex.printStackTrace();
                        } catch (Exception ex1) {
                            Logger.getLogger(KeepPingingLeader.class.getName()).log(Level.SEVERE, null, ex1);
                        } 
                    } catch (IOException ex) {
                        Logger.getLogger(KeepPingingLeader.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(KeepPingingLeader.class.getName()).log(Level.SEVERE, null, ex);
                    } 
                }else if(Info.MainLeader.equalsIgnoreCase(InetAddress.getLocalHost().getHostAddress().toString())){
                    break;
                }
            } catch (UnknownHostException ex) {
                Logger.getLogger(KeepPingingLeader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
