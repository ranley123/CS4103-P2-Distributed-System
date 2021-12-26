import java.util.ArrayList;

public class PostQueue {
    public static ArrayList<Post> posts = new ArrayList<>();

    public static Post getPostByReceiver(int receiver){
        Post res = null;
        for(int i = 0; i < posts.size(); i++){
            if(posts.get(i).getReceiver() == receiver){
                res = posts.get(i);
                posts.remove(i);
                return res;
            }
        }
        return null;
    }

    public static void addPost(int sender, int receiver, String msg){
        Post newPost = new Post(sender, receiver, msg);
        posts.add(newPost);
    }

}
