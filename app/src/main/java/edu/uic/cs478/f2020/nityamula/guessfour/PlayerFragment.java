package edu.uic.cs478.f2020.nityamula.guessfour;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class PlayerFragment extends Fragment {

    ListView listView;
    ArrayList<PlayerStatusBundle> opponentPlayerResponse;
    View view;

    public PlayerFragment(ArrayList<PlayerStatusBundle> opponentPlayerResponse){
        this.opponentPlayerResponse = opponentPlayerResponse;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.player_status_board, container, false);

        listView = (ListView) view.findViewById(R.id.status_list_view);
        Adapter customAdapter = new Adapter(getContext(), R.layout.status_item,this.opponentPlayerResponse);
        listView.setAdapter(customAdapter);

        return view;
    }
}
