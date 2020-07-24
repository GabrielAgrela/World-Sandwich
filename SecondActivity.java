package com.WeDev.worldsandwich;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.TimerTask;

public class SecondActivity extends AppCompatActivity
{
    GoogleSignInClient mGoogleSignInClient;
    ImageView imageView;
    LinearLayout msgBoxLayout;
    ScrollView scrollView;
    TextView name, email, id;
    EditText msgContent;
    Button signOut,sendBtn;
    String personId,buddyId,maxMsgID="0",userIDinMsg="1",buddyIDinMsg="2";
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        // firebase/google setups
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        imageView = findViewById(R.id.imageView);
        name = findViewById(R.id.textName);
        email = findViewById(R.id.textEmail);
        id = findViewById(R.id.textID);
        msgContent = findViewById(R.id.msgContent);
        msgBoxLayout = findViewById(R.id.msgBoxContainer);
        signOut = findViewById(R.id.button);
        sendBtn = findViewById(R.id.sendBtn);
        scrollView = findViewById(R.id.scrollView);
        signOut.setOnClickListener(new View.OnClickListener() 
        {
            @Override
            public void onClick(View view) 
            {
                switch (view.getId()) 
                {
                    case R.id.button:
                        signOut();
                        break;
                }
            }
        });
        
        //send message
        sendBtn.setOnClickListener(new View.OnClickListener() 
        {

            @Override
            public void onClick(View view)
            {
                newMsg(msgContent.getText().toString());
            }
        });
        
        //on auth get google data
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null)
        {
            String personName = acct.getDisplayName();
            String personEmail = acct.getEmail();
            personId = acct.getId();
            Uri personPhoto = acct.getPhotoUrl();

            //adds acc to firebasedb if doesnt exist yet (name and google ID)
            newAcc(personName);
            
            //TODO: automatic pairing (find unpaired buddy in "users" with the oposite location value to this user and pair them on "pairs")
            
            //finds user paired buddy
            getUserPair();
            
            // header info for now
            name.setText(personName);
            email.setText(personEmail);
            id.setText(personId);
            Glide.with(this).load(String.valueOf(personPhoto)).into(imageView);
            
            //updates chat container everytime data changes in firebasedb
            updateChat();

        }
    }

    private void newAcc(String personName)
    {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("users").child(personId).child("name");

        myRef.setValue(personName);
    }

    private void newMsg(String msgTxt)
    {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("chat").child(personId).child(String.valueOf(Integer.parseInt(maxMsgID)+1)).child("user").child(userIDinMsg).child("msg");
        myRef.setValue(msgTxt);

        myRef = database.getReference().child("chat").child(buddyId).child(String.valueOf(Integer.parseInt(maxMsgID)+1)).child("user").child(buddyIDinMsg).child("msg");
        myRef.setValue(msgTxt);

        msgContent.setText("");

    }

    private void getUserPair() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("pairs").child(personId);
        myRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                try
                {
                    if (dataSnapshot.exists())
                    {
                        for(DataSnapshot userGoogleIDBranch : dataSnapshot.getChildren())
                        {
                            if(!userGoogleIDBranch.getKey().equals(personId))
                            {
                                buddyId = userGoogleIDBranch.getKey();
                                Log.d("Pair Tree", "buddy id: " + buddyId);
                            }
                        }
                    }
                    else {
                        Log.d("Pair Tree", "Doesnt exist");
                    }
                }
                catch(Exception e)
                {
                    System.out.println("The read failed: É NULL");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    //gets alll data from each branch inside branch until it gets to the message String
    private void updateChat()
    {
        DatabaseReference mref;
        mref = FirebaseDatabase.getInstance().getReference("chat").child(personId);
        mref.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                LinearLayout cleanLayout = findViewById(R.id.msgBoxContainer);
                cleanLayout.removeAllViews();
                String msgID,user,userID,msgText;
                try
                {
                    if (dataSnapshot.exists())
                    {
                        for(DataSnapshot msgIDBranch : dataSnapshot.getChildren())
                        {
                            msgID = msgIDBranch.getKey();
                            Log.d("MSG Tree", "MSG ID: " + msgID);
                            maxMsgID=msgID;
                            for(DataSnapshot userBranch : msgIDBranch.getChildren())
                            {
                                user = userBranch.getKey();
                                Log.d("MSG Tree", " child: " + user);
                                for(DataSnapshot userIDBranch : userBranch.getChildren())
                                {
                                    userID = userIDBranch.getKey();
                                    Log.d("MSG Tree", "userID: " + userID);
                                    for(DataSnapshot msgBranch : userIDBranch.getChildren())
                                    {
                                        //new chat message layout works like this: TV (chat message string) inside small layout (small container with margins and stuff) inside bigger layout (chat location)

                                        //message String
                                        msgText = msgBranch.getValue(String.class);
                                        //new TV (chat message)
                                        TextView msgBoxTV = new TextView(getApplicationContext());
                                        //new Layout
                                        LinearLayout msgBoxTVContainer = new LinearLayout(getApplicationContext());
                                        //new Layout parameters (wrap content, margins, left/right side depending on the user sender/receiver)
                                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0);
                                        lp.setMargins(20,20,20,20);
                                        if (userID.contains("1"))
                                            lp.gravity = Gravity.RIGHT;
                                        else
                                            lp.gravity = Gravity.LEFT;
                                        msgBoxTVContainer.setLayoutParams(lp);
                                        //chat message parameters
                                        msgBoxTV.setTextAppearance(getApplicationContext(), android.R.style.TextAppearance_Large);
                                        msgBoxTV.setText(msgText);
                                        msgBoxTV.setBackgroundResource(R.drawable.rounded_corner1);
                                        //packing TV's to Layout and layout to layout
                                        msgBoxTVContainer.addView(msgBoxTV);
                                        msgBoxLayout.addView(msgBoxTVContainer);
                                        //chat message padding
                                        msgBoxTV.setPadding(20,20,20,20);

                                        Log.d("MSG Tree", "MSG text: " + msgText);
                                        scrollToBottom();
                                    }

                                }
                            }
                        }
                    }
                    else{
                        Log.d("MSG Tree", "Doesnt exist");
                    }
                }
                catch(Exception e)
                {
                    System.out.println("The read failed: É NULL");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    private void signOut()
    {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        Toast.makeText(SecondActivity.this, "out", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void scrollToBottom()
    {
        scrollView.post(new Runnable()
        {
            @Override
            public void run()
            {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }
}