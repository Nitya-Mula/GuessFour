package edu.uic.cs478.f2020.nityamula.guessfour;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class Adapter extends ArrayAdapter<PlayerStatusBundle> {
    ArrayList<PlayerStatusBundle> numbers;

    public Adapter(Context context, int textViewResourceId, ArrayList<PlayerStatusBundle> objects) {
        super(context, textViewResourceId, objects);
        numbers = objects;
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.status_item, null);
        TextView textView1 = (TextView) v.findViewById(R.id.guessed_number);
        TextView textView2 = (TextView) v.findViewById(R.id.correct_pos_count);
        TextView textView3 = (TextView) v.findViewById(R.id.incorrect_pos_count);
        TextView textView4 = (TextView) v.findViewById(R.id.one_wrong_num);

        textView1.setText(numbers.get(position).getGuessedNum());
        textView2.setText(String.valueOf(numbers.get(position).getCorrectPosCount()));
        textView3.setText(String.valueOf(numbers.get(position).getWrongPosCount()));

        int residue = numbers.get(position).getWrongNumber();
        textView4.setText(String.valueOf(residue == 999 ? "XX" : residue));

        return v;

    }
}

