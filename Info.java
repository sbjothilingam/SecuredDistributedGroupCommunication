
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Suresh */
public class Info {
    static Boolean isLeaderElectionHappening=false;
    static String ownIp;
    static String AuthenticatorIp;
    static int port=1235;
    static String SessionKey;
    static String JoinKey;
    static String LeaderIp; // for joining a group
    static String MainLeader;
    static String MainLeaderGroup;
    static String Id;
    // for seperate comm
    static String communicationKey;
    static String communicationIp;
    static List<String> ListOfGroupId= new ArrayList();
    
    //for server
    static String LeadersCommKey;
    static String LeadersJoinKey;
    static List<String> GroupNodesList= new ArrayList();
    static List<String> GroupNodesListId= new ArrayList();
    Info(){
        try {
            ownIp = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
