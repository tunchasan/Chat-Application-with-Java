package chat.app.Server;

import chat.app.DB.UserDataHandler;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class ServerTask implements Runnable {
    
    private Socket userSocket; 
    private String userName;
    private Scanner in; //receiver
    private PrintWriter out; //sender
    private UserDataHandler userData;
    
    ServerTask(Socket userSocket) {
        this.userSocket = userSocket;
        this.userName = "";
    }
    
    @Override
    public void run() {
         System.out.println("Connected: " + userSocket);
             try {
                  in = new Scanner(userSocket.getInputStream());
                  
                  out = new PrintWriter(userSocket.getOutputStream(), true);
                  // Set client name as unique
                  SetUniqueUserName();
                  // Now successful name has been chosen , we can start core process to manage client requests.
                  while (true) {
                        String receiver = in.nextLine();
                        // Client connection to end, if write and send "/quit" to server
                        if (receiver.toLowerCase().startsWith("/quit")) {
                           return;
                        }
                        // if client's username didn't assing.
                        if (receiver.equals("/singleUser")){ // Message to single user Action
                           out.println("Type receiver's user name"); 
                           while(true){
                               receiver = in.nextLine();
                               
                               if(receiver.isBlank()){
                                   out.println("User name can not be empty. Try again."); 
                               }
                               else{
                                   if (ChatServer.GetNameList().contains(receiver)){
                                      String receiverName = receiver;
                                      
                                      out.println(Messages.Results.Type_your_message.toString());
                                      
                                      while(true){
                                          String message = in.nextLine();
                                          
                                          if(message.isBlank()){
                                            out.println("User name can not be empty. Try again."); 
                                          }
                                          else{
                                              //Request to server to send message
                                              String result = ChatServer.SendMessageToPerson(message, receiverName, userName);
                                              // Send request result message to client
                                              out.println(result);
                                              break;
                                          }
                                    }
                                      break;
                                      
                                    } else{
                                       out.println(Messages.Results.User_not_exist.toString() + " Try again."); 
                                   }
                               }
                           }
                        }
                        else if (receiver.equals("/group")){ // Message to a group Action
                           if (userData.GetGroup().GetGroupSize() == 0){
                                out.println("Create a group for sending message to group");
                           }
                           else{
                               out.println(Messages.Results.Type_your_message.toString());
                               while (true) {
                                  // Get client request
                                    receiver = in.nextLine();

                                    if (receiver.isBlank()){
                                        out.println("Message can not be empty. Try again"); 
                                    }  
                                    else{
                                        userData.GetGroup().SendMessageToGroup(receiver, userName);
                                        out.println("Message sended successfuly."); 
                                        break;
                                    }
                               }
                           }
                        }
                        else if (receiver.equals("/createGroup")){ // Message to a group Action
                           
                           if (userData.GetGroup().GetGroupSize() == 0){
                               out.println("Type group name.");
                               while(true){
                                    // Get client request
                                    receiver = in.nextLine();

                                    if (receiver.isBlank()){
                                        out.println("Group name can not be empty. Try again."); 
                                    } 
                                    else{
                                        // User group initializition
                                        userData.GetGroup().CreateGroup(receiver);
                                        
                                        out.println("Add group to member. Type user names. To complete, write /done."); 
                                        while (true){
                                             receiver = in.nextLine();
                                             if (receiver.startsWith("/done")){
                                                 if (userData.GetGroup().GetGroupSize() > 0) {
                                                      userData.GetGroup().AddUserToGroup(out);
                                                      // Update group information for other members
                                                      for (UserDataHandler data:ChatServer.GetUserList()){
                                                          if (userData.GetGroup().GetGroupList().contains(data.GetWriter())){
                                                               // Update group member group data
                                                               data.GetGroup().SettGroupName(userData.GetGroup().GetGroupName());
                                                               
                                                               data.GetGroup().SetGroupList(userData.GetGroup().GetGroupList());
                                                          }
                                                      }
                                                      out.println("Group created succesfully."); 
                                                      break;
                                                 }
                                                 else{
                                                      out.println("To done, at least you need to add a user."); 
                                                 }
                                             }
                                             
                                             if (receiver.isBlank()){
                                                 out.println("User name can not be empty. Try again."); 
                                             }
                                             else if (ChatServer.GetNameList().contains(receiver)) {
                                                 for(UserDataHandler data:ChatServer.GetUserList()){
                                                     if (data.GetName().equals(receiver)){
                                                         userData.GetGroup().AddUserToGroup(data.GetWriter());
                                                         out.println(receiver + " added to group"); 
                                                         break;
                                                     }
                                                 }
                                             }
                                             else{
                                                 out.println("User does not exist. Try again."); 
                                             }
                                         }
                                        break;
                                    } 
                                }
                           }
                           else{
                               out.println("You are already a member of a group.."); 
                           }
                        }
                        else if (receiver.startsWith("/allUser")){ // Message to all user Action
                           out.println(Messages.Results.Type_your_message.toString());  
                           while(true){
                               // Get client request
                               receiver = in.nextLine();
                               
                               if(receiver.isBlank()){
                                   out.println("Message can not be empty. Try again."); 
                               } 
                               else{
                                   break;
                               }
                           }
                           String result = ChatServer.SendMessageToAll(receiver, userName);
                           
                           out.println(result);
                        }
                        else{
                           out.println("Wrong action!");
                        }
                  }
                  
         } catch (Exception e) {
                  System.out.println("Error:" + userSocket); } 
                  
             finally {
             try {
                    userSocket.close();
                  } catch (IOException e) { }
                  System.out.println("Closed: " + userSocket);
         }         
    }  
    
    // Sets Client's Unique Username
    private synchronized void SetUniqueUserName(){
        out.println("Enter a user name:");
        while(true) {
            String name = in.nextLine();
            if (name.isBlank()){
               out.println("Username can not be empty");
            }
            else if (!(ChatServer.GetNameList().contains(name))){
                // Add name to names list in server
                ChatServer.GetNameList().add(name);
                //Create new data object to store user information
                userData = new UserDataHandler(name, out);
                //Add data to list
                ChatServer.GetUserList().add(userData);
                // Assing unique name to userName
                userName = name;
                // Sends return message to client
                out.println("Connected Server as " + userName);
                //Then finalize the process
                return;
            }
            else{
                out.println("The username is already using by another user");
            }
        }             
    } 
}