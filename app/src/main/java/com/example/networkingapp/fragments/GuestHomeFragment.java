package com.example.networkingapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.networkingapp.MyAdapter;
import com.example.networkingapp.PostsClass;
import com.example.networkingapp.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GuestHomeFragment extends Fragment {

    FirebaseAuth mAuth;
    FirebaseUser user;
    FirebaseDatabase database;
    DatabaseReference postsRef;
    DatabaseReference usersRef;
    TextInputEditText editTextName;
    MaterialButtonToggleGroup homeToggleButton;
    Button btnNewPosts, btnYourSub;
    RecyclerView postsRV;
    List<PostsClass> postsList;
    TextView noSubscriptionsTextView;
    MyAdapter adapter;
    boolean showAllPosts = true; // Флаг, определяющий, нужно ли показывать все посты или только посты подписок

    public GuestHomeFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_guest_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        postsRef = database.getReference("Posts");
        usersRef = database.getReference("Users");

        editTextName = view.findViewById(R.id.editTextName);
        homeToggleButton = view.findViewById(R.id.homeToggleButton);
        btnNewPosts = view.findViewById(R.id.btnNewPosts);
        btnYourSub = view.findViewById(R.id.btnYourSub);
        noSubscriptionsTextView = view.findViewById(R.id.no_subscriptions_text_view);
        postsRV = view.findViewById(R.id.postsRV);

        postsRV.setLayoutManager(new LinearLayoutManager(getActivity()));
        postsList = new ArrayList<>();
        adapter = new MyAdapter(getActivity(), postsList);
        postsRV.setAdapter(adapter);

        btnNewPosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllPosts = true;
                loadPosts();
            }
        });

        btnYourSub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAllPosts = false;
                loadPosts();
            }
        });

        // По умолчанию показываем все посты
        loadPosts();

        return view;
    }

    private void loadPosts(){
        adapter.clear();
        if(showAllPosts){
            //Загружаем все посты
            Query query = postsRef.orderByChild("timestamp").limitToLast(100); // Пример: загрузить 100 последних постов
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()){
                        PostsClass post = ds.getValue(PostsClass.class);
                        postsList.add(post);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(requireContext(), "Не удалось загрузить данные", Toast.LENGTH_SHORT).show();
                }
            });
        } else{
            // Загружаем посты подписок текущего пользователя
            Query query = usersRef.child(user.getUid()).child("subscriptions");
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(!snapshot.exists()){
                        // Если список подписок не существует или пуст, показываем сообщение об отсутствии подписок
                        showNoSubscriptionsMessage();
                    }else{
                        // Если список подписок существует, загружаем посты подписок текущего пользователя
                        List<String> subscriptions = new ArrayList<>();
                        for(DataSnapshot ds : snapshot.getChildren()){
                            subscriptions.add(ds.getKey());
                        }
                        loadPostsForSubscriptions(subscriptions);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getActivity(), "Не удалось загрузить данные", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadPostsForSubscriptions(final List<String> subscriptions){
        adapter.clear();
        for(final String userID : subscriptions){
            Query query = postsRef.orderByChild("authorID").equalTo(userID);
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for(DataSnapshot ds : snapshot.getChildren()){
                        PostsClass post = ds.getValue(PostsClass.class);
                        postsList.add(post);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getActivity(), "Не удалось загрузить данные", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showNoSubscriptionsMessage(){
        noSubscriptionsTextView.setVisibility((View.VISIBLE));
        postsRV.setVisibility(View.GONE);
    }
}