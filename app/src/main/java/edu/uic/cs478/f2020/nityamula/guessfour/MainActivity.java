package edu.uic.cs478.f2020.nityamula.guessfour;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // List of Objects that contain guesses and responses for each player
    public static ArrayList<PlayerStatusBundle> player1_reply_bundle = new ArrayList<>();
    public static ArrayList<PlayerStatusBundle> player2_reply_bundle = new ArrayList<>();

    // Arrays to store previous guess responses from opponent thread
    int[] resultP1;
    int[] resultP2;

    // Lists to store digits based on the context
    List<Integer> allowedDigitsForP1Guess = new ArrayList<>(Arrays.asList(new Integer[]{0,1,2,3,4,5,6,7,8,9}));
    List<Integer> allowedDigitsForP2Guess = new ArrayList<>(Arrays.asList(new Integer[]{9,8,7,6,5,4,3,2,1,0}));
    List<Integer> allDigitsList = new ArrayList<>();

    // Stores the number guessed by players to discard duplicate guesses
    ArrayList<Integer> guessNumbersByP1 = new ArrayList<Integer>();
    ArrayList<Integer> guessNumbersByP2 = new ArrayList<Integer>();

    // Constants for communication between threads
    private static final int GAME_START = 1;
    private static final int NEXT_GUESS = 2;
    private static final int UPDATE_UI_FOR_1 = 3;
    private static final int UPDATE_UI_FOR_2 = 4;

    private static final int GET_GUESS_FROM_P1 = 5;
    private static final int GET_GUESS_FROM_P2 = 6;

    private static final int GET_RESPONSE_FROM_P1 = 7;
    private static final int GET_RESPONSE_FROM_P2 = 8;

    private static final int GET_SECRET_NUMBER = 9;
    private static final int DECIDE_SECRET_NUMBER = 10;

    private static final int GET_SECRET_FROM_1 = 11;
    private static final int GET_SECRET_FROM_2 = 12;

    private static final String RESULT_BUNDLE_P1 = "result_bundle_p1";
    private static final String RESULT_BUNDLE_P2 = "result_bundle_p2";

    private static final String RESPONSE_BUNDLE_P1 = "response_bundle_p1";
    private static final String RESPONSE_BUNDLE_P2 = "response_bundle_p2";

    int winner = 999;

    // Actual number of the players
    int[] player1NumberAsArray, player2NumberAsArray;
    int player1NumberAsInt, player2NumberAsInt;

    // Global counter not to exceed 20 iterations
    int iterations1, iterations2;

    // Text View ids for UI Thread to make updates
    TextView gameResult, tvActualNumber1, tvActualNumber2, iterations;

    // Handlers and Threads initialization
    static Handler handler;
    private Handler handlerP1, handlerP2;
    Thread threadPlayer1, threadPlayer2;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        for(int i = 0; i <= 9; i++)
            allDigitsList.add(i);

        iterations1 = 20;
        iterations2 = 20;

        tvActualNumber1 = (TextView) findViewById(R.id.actual_number_1);
        tvActualNumber2 = (TextView) findViewById(R.id.actual_number_2);
        iterations = (TextView) findViewById(R.id.iterations);
        gameResult = (TextView) findViewById(R.id.game_result);

        loadFragment(new PlayerFragment(player1_reply_bundle), R.id.frame_layout_player_1);
        loadFragment(new PlayerFragment(player2_reply_bundle), R.id.frame_layout_player_2);

        // Handler for UI Thread
        handler = new Handler(Looper.myLooper()){
            @Override
            public void handleMessage(Message msg){
                Message message;
                switch (msg.what){

                    // Worker threads to decide secret number
                    case DECIDE_SECRET_NUMBER:{
                        Log.i("MainReset", "UI Thread decide secret");
                        message = handlerP1.obtainMessage(GET_SECRET_NUMBER);
                        handlerP1.sendMessage(message);
                        break;
                    }

                    // Getting the secret number from P1 to update in UI
                    case GET_SECRET_FROM_1:{
                        Log.i("MainReset", "Received secret from P1");
                        player1NumberAsInt = displayGeneratedNumber(tvActualNumber1, player1NumberAsArray);
                        message = handlerP2.obtainMessage(GET_SECRET_NUMBER);
                        handlerP2.sendMessage(message);
                        break;
                    }

                    // Getting the secret number from P2 to update in UI
                    case GET_SECRET_FROM_2:{
                        Log.i("MainReset", "Received secret from P2");
                        player2NumberAsInt = displayGeneratedNumber(tvActualNumber2, player2NumberAsArray);
                        message = handler.obtainMessage(GAME_START);
                        handler.sendMessage(message);
                        break;
                    }

                    // Send Message to player 1 to start the game
                    case GAME_START:{
                        gameResult.setText("Game started");
                        Log.i("MainThread", "Game started");
                        message = handlerP1.obtainMessage(NEXT_GUESS);
                        handlerP1.sendMessage(message);
                        break;
                    }

                    // Update player 1's UI with its guess
                    case UPDATE_UI_FOR_1:{
                        resultP1 = msg.getData().getIntArray(RESULT_BUNDLE_P1);
                        String guessAsString = String.valueOf(resultP1[0]);
                        guessAsString = guessAsString.length() == 3 ? "0" + guessAsString : guessAsString;
                        displayPlayer1Result(guessAsString, resultP1);
                        Log.i("MainThread", "Move 1 updated");

                        if(iterations1 == 0 && iterations2 == 0)
                        {
                            Log.i("MainThread", "Exited Game");
                            cleanUpResources();
                            gameResult.setText("DRAW MATCH");
                            gameResult.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in));
                            break;
                        }
                        if (winner == 1) {
                            Log.i("MainThread", "Exited Game");
                            cleanUpResources();
                            gameResult.setText("PLAYER 1 WINS");
                            gameResult.setTextColor(getColor(R.color.green));
                            gameResult.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in));
                            break;
                        }

                        // Making player 2 think for 2 seconds before guessing by posting runnable
                        handlerP2.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Log.i("Thread 1", " Sleeping");
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        // Player 2 has to make the next guess
                        message = handlerP2.obtainMessage(NEXT_GUESS);
                        handlerP2.sendMessage(message);
                        break;
                    }

                    // Update player 2's UI with its guess
                    case UPDATE_UI_FOR_2:{
                        resultP2 = msg.getData().getIntArray(RESULT_BUNDLE_P2);
                        String guessAsString = String.valueOf(resultP2[0]);
                        guessAsString = guessAsString.length() == 3 ? "0" + guessAsString : guessAsString;
                        displayPlayer2Result(guessAsString, resultP2);
                        Log.i("MainThread", "Move 2 updated");

                        if(iterations1 ==0 && iterations2 ==0)
                        {
                            Log.i("MainThread", "Exited Game");
                            cleanUpResources();
                            gameResult.setText("DRAW MATCH");
                            gameResult.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in));
                            break;
                        }

                        if( winner == 2){
                            Log.i("MainThread","Exited Game");
                            cleanUpResources();
                            gameResult.setText("PLAYER 2 WINS");
                            gameResult.setTextColor(getColor(R.color.red));
                            gameResult.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in));
                            break;
                        }

                        // Making player 1 think for 2 seconds before guessing
                        handlerP1.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Log.i("Thread 2", " Sleeping");
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        // Player 1 has to make the next guess
                        message = handlerP1.obtainMessage(NEXT_GUESS);
                        handlerP1.sendMessage(message);
                        break;
                    }
                    default:
                        break;
                }   // End of switch
            }   // End of handleMessage
        };  // End of Handler


        // Setting click listener for start button
        findViewById(R.id.start_game_button).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (threadPlayer1!=null && threadPlayer2!=null)
                    cleanUpResources();

                resetGame();

                //Thread for Player 1
                threadPlayer1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        // Handler for Player 1
                        handlerP1 = new Handler(Looper.myLooper()){
                            @Override
                            public void handleMessage(Message msg) {
                                Message message;
                                switch (msg.what){

                                    case GET_SECRET_NUMBER:{
                                        Log.i("MainReset", "Secret generating for Thread1");
                                        player1NumberAsArray = generateRandomNumber(allDigitsList);
                                        message = handler.obtainMessage(GET_SECRET_FROM_1);
                                        handler.sendMessage(message);
                                        break;
                                    }

                                    case NEXT_GUESS:{
                                        message = handlerP2.obtainMessage(GET_GUESS_FROM_P1);
                                        message.arg1 = makeAGuess1();
                                        Log.i("Main", "Making the guess by P1" + String.valueOf(message.arg1));
                                        handlerP2.sendMessage(message);
                                        break;
                                    }

                                    case GET_GUESS_FROM_P2:{
                                        int guessP2 = msg.arg1;
                                        int[] responseForP2Guess = generateResponseForP2Guess(guessP2);
                                        Bundle responseBundle = new Bundle();
                                        message = handlerP2.obtainMessage(GET_RESPONSE_FROM_P1);
                                        Log.i("MainThread", "The response from P2 " + String.valueOf(responseForP2Guess[0]) + " " + String.valueOf(responseForP2Guess[1]) + " " + String.valueOf(responseForP2Guess[2]) + " " + String.valueOf(responseForP2Guess[3]));
                                        responseBundle.putIntArray(RESPONSE_BUNDLE_P1, responseForP2Guess);
                                        message.setData(responseBundle);
                                        handlerP2.sendMessage(message);
                                        break;
                                    }

                                    case GET_RESPONSE_FROM_P2:{
                                        int[] responseFromP2 = msg.getData().getIntArray(RESPONSE_BUNDLE_P2);
                                        Log.i("MainThread", "Response from P2 received by P1 " + String.valueOf(responseFromP2[0]) + " " + String.valueOf(responseFromP2[1]) + " " + String.valueOf(responseFromP2[2]) + " " + String.valueOf(responseFromP2[3]) + " ");
                                        Bundle resultBundleP1 = new Bundle();
                                        message = handler.obtainMessage(UPDATE_UI_FOR_1);
                                        resultBundleP1.putIntArray(RESULT_BUNDLE_P1, responseFromP2);
                                        message.setData(resultBundleP1);
                                        handler.sendMessage(message);
                                        break;
                                    }
                                    default:
                                        break;
                                }
                            }
                        };
                        Log.i("P1", "Handler created");

                        // Starting the game by sending message to UI Thread
                        Message message = handler.obtainMessage(DECIDE_SECRET_NUMBER);
                        handler.sendMessage(message);
                        Looper.loop();
                    }
                });   // End of Player1 thread

                //Thread for Player 2
                threadPlayer2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();

                        // Handler for Player 1
                        handlerP2 = new Handler(Looper.myLooper()){
                            @Override
                            public void handleMessage(Message msg) {
                                Message message;
                                switch (msg.what){

                                    case GET_SECRET_NUMBER:{
                                        Log.i("MainReset", "Secret generating for Thread2");
                                        player2NumberAsArray = generateRandomNumber(allDigitsList);
                                        message = handler.obtainMessage(GET_SECRET_FROM_2);
                                        handler.sendMessage(message);
                                        break;
                                    }


                                    case NEXT_GUESS:{
                                        message = handlerP1.obtainMessage(GET_GUESS_FROM_P2);
                                        message.arg1 = makeAGuess2();
                                        Log.i("MainThread", "Making the guess by P2" + String.valueOf(message.arg1));
                                        handlerP1.sendMessage(message);
                                        break;
                                    }

                                    case GET_GUESS_FROM_P1:{
                                        int guessP1 = msg.arg1;
                                        int[] responseForP1Guess = generateResponseForP1Guess(guessP1);
                                        Bundle responseBundle = new Bundle();
                                        message = handlerP1.obtainMessage(GET_RESPONSE_FROM_P2);
                                        Log.i("MainThread", "The response from P2 " + String.valueOf(responseForP1Guess[0]) + " " + String.valueOf(responseForP1Guess[1]) + " " + String.valueOf(responseForP1Guess[2]) + " " + String.valueOf(responseForP1Guess[3]));
                                        responseBundle.putIntArray(RESPONSE_BUNDLE_P2, responseForP1Guess);
                                        message.setData(responseBundle);
                                        handlerP1.sendMessage(message);
                                        break;

                                    }

                                    case GET_RESPONSE_FROM_P1:{
                                        int[] responseFromP1 = msg.getData().getIntArray(RESPONSE_BUNDLE_P1);
                                        Log.i("MainThread", "Response from P2 received by P1 " + String.valueOf(responseFromP1[0]) + " " + String.valueOf(responseFromP1[1]) + " " + String.valueOf(responseFromP1[2]) + " " + String.valueOf(responseFromP1[3]) + " ");
                                        Bundle resultBundleP2 = new Bundle();
                                        message = handler.obtainMessage(UPDATE_UI_FOR_2);
                                        resultBundleP2.putIntArray(RESULT_BUNDLE_P2, responseFromP1);
                                        message.setData(resultBundleP2);
                                        handler.sendMessage(message);
                                        break;
                                    }
                                    default:{
                                        break;
                                    }
                                }
                            }
                        };
                        Log.i("P2", "Handler created");
                        Looper.loop();
                    }
                });  // End of Player2 thread
                Log.i("MainReset", "Start Thread 1");
                threadPlayer1.start();
                Log.i("MainReset", "Start Thread 2");
                threadPlayer2.start();
            }   // End of onClick method
        });   // End of adding click listener
    }   // End of onCreate

    //Function to update UI with the secret number of P1
    public void displayPlayer1Result(String numberStr, int[] result){
        player1_reply_bundle.add(new PlayerStatusBundle(numberStr, result[1],result[2],result[3]));
        iterations1--;
        iterations.setText("Iteration " + String.valueOf(20 - iterations1) + " for Player 1");
        loadFragment(new PlayerFragment(player1_reply_bundle), R.id.frame_layout_player_1);
    }

    //Function to update UI with the secret number of P2
    public void displayPlayer2Result(String numberStr, int[] result){
        player2_reply_bundle.add(new PlayerStatusBundle(numberStr, result[1],result[2],result[3]));
        iterations2--;
        iterations.setText("Iteration " + String.valueOf(20 - iterations2) + " for Player 2");
        loadFragment(new PlayerFragment(player2_reply_bundle), R.id.frame_layout_player_2);
    }

    // Reset on clicking the start button
    public void resetGame(){
        Log.i("MainReset", "Reset game");
        gameResult.clearAnimation();

        player1_reply_bundle.removeAll(player1_reply_bundle);
        player2_reply_bundle.removeAll(player2_reply_bundle);

        iterations2 = 20;
        iterations1 = 20;

        loadFragment(new PlayerFragment(player1_reply_bundle), R.id.frame_layout_player_1);
        loadFragment(new PlayerFragment(player2_reply_bundle), R.id.frame_layout_player_2);

        tvActualNumber1.setText("xxxx");
        tvActualNumber2.setText("xxxx");

        gameResult.setText("");
        gameResult.setTextColor(Color.BLUE);
        gameResult.setBackgroundResource(0);
        iterations.setText("");

        winner = 999;

        allowedDigitsForP1Guess.clear();
        allowedDigitsForP2Guess.clear();

        resultP1 = null;
        resultP2 = null;

        guessNumbersByP1.clear();
        guessNumbersByP2.clear();

        allowedDigitsForP1Guess = new ArrayList<>(Arrays.asList(new Integer[]{0,1,2,3,4,5,6,7,8,9}));
        allowedDigitsForP2Guess = new ArrayList<>(Arrays.asList(new Integer[]{9,8,7,6,5,4,3,2,1,0}));

        Log.i("MainReset", "All digits    :" + allDigitsList.size());
        Log.i("MainReset", "Allowed by 1,2   :" + allowedDigitsForP1Guess.size() + ", " + allowedDigitsForP2Guess.size());
        Log.i("MainReset", "guessNumbers by 1,2  :" + guessNumbersByP1.size() + ", " + guessNumbersByP2.size());
    }

    // Clean up the resources before reusing for new game
    public void cleanUpResources(){
        threadPlayer1.interrupt();
        threadPlayer2.interrupt();
        Log.i("MainReset", "Threads cleaned");

        handler.removeCallbacksAndMessages(null);
        handlerP1.removeCallbacksAndMessages(null);
        handlerP2.removeCallbacksAndMessages(null);
        Log.i("MainReset", "Removed callbacks");

        handlerP1.getLooper().quitSafely();
        handlerP2.getLooper().quitSafely();
        Log.i("MainReset", "Exited Loopers");
    }

    // Loads fragments whenever a new guess is make
    public void loadFragment(Fragment fragment, int resourceId) {
        androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(resourceId, fragment);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
    }

    // P2's function to generate response for P1's guess
    public int[] generateResponseForP1Guess(int guessP1){
        int temp = guessP1;
        int[] guessP1Array = new int[4];
        HashSet<Integer> actualP2Number = new HashSet<>();
        HashSet<Integer> incorrectDigits = new HashSet<>();
        int correct_pos = 0, incorrect_pos = 0, wrong_number = 999;

        for(int i = 0; i < 4; i++){
            guessP1Array[3-i] = temp % 10;
            temp = temp / 10;
            actualP2Number.add(player2NumberAsArray[i]);
        }

        for(int i = 0; i < 4; i++){
            if(actualP2Number.contains(guessP1Array[i])){
                if(guessP1Array[i] == player2NumberAsArray[i]){
                    correct_pos++;
                }
                else{
                    incorrect_pos++;
                }
            }
            else{
                incorrectDigits.add(guessP1Array[i]);
            }
        }

        Iterator itr = incorrectDigits.iterator();
        if(itr.hasNext())
            wrong_number = (int) itr.next();
        else
            wrong_number = 999;
        if(correct_pos == 4)
            winner = 1;
        return new int[]{guessP1, correct_pos, incorrect_pos, wrong_number};
    }

    // P1's function to generate response for P2's guess
    public int[] generateResponseForP2Guess(int guessP2){
        int temp = guessP2;
        int[] guessP2Array = new int[4];
        HashSet<Integer> actualP1Number = new HashSet<>();
        HashSet<Integer> incorrectDigits = new HashSet<>();
        int correct_pos = 0, incorrect_pos = 0, wrong_number = 999;

        for(int i = 0; i < 4; i++){
            guessP2Array[3-i] = temp % 10;
            temp = temp / 10;
            actualP1Number.add(player1NumberAsArray[i]);
        }

        for(int i = 0; i < 4; i++){
            if(actualP1Number.contains(guessP2Array[i])){
                if(guessP2Array[i] == player1NumberAsArray[i]){
                    correct_pos++;
                }
                else{
                    incorrect_pos++;
                }
            }
            else{
                incorrectDigits.add(guessP2Array[i]);
            }
        }

        Iterator itr = incorrectDigits.iterator();
        if(itr.hasNext())
            wrong_number = (int) itr.next();
        else
            wrong_number = 999;

        if(correct_pos == 4)
            winner = 2;
        return new int[]{guessP2, correct_pos, incorrect_pos, wrong_number};
    }

    // P1's guessing strategy
    public int makeAGuess1(){
        int[] temp;
        int res = 0;
        boolean noRepetetion = true;
        if(resultP1 != null && resultP1[3] == 999){
            allowedDigitsForP1Guess.clear();
            allowedDigitsForP1Guess.add(resultP1[0]%10);
            allowedDigitsForP1Guess.add((resultP1[0]/10)%10);
            allowedDigitsForP1Guess.add((resultP1[0]/100)%10);
            allowedDigitsForP1Guess.add((resultP1[0]/1000)%10);
            Collections.shuffle(allowedDigitsForP1Guess);
            while(noRepetetion){
                Log.i("MainReset", "Entering 1 with allowedLength1 " + allowedDigitsForP1Guess.get(0) + " " + allowedDigitsForP1Guess.get(1) + " " + allowedDigitsForP1Guess.get(2) + " " + allowedDigitsForP1Guess.get(3));
                temp = generateRandomNumber(allowedDigitsForP1Guess);
                res = arrayToInteger(temp);
                Integer resObj = new Integer(res);
                if(!guessNumbersByP1.contains(resObj)){
                    guessNumbersByP1.add(resObj);
                    noRepetetion = false;
                    return res;
                }
            }
        }
        if(resultP1 != null && allowedDigitsForP1Guess.contains(resultP1[3])){
            Log.i("MainReset", "Entering 2 with allowedLength1 " + allowedDigitsForP1Guess.get(0) + " " + allowedDigitsForP1Guess.get(1) + " " + allowedDigitsForP1Guess.get(2) + " " + allowedDigitsForP1Guess.get(3));
            allowedDigitsForP1Guess.remove(new Integer(resultP1[3]));
        }

        while(noRepetetion && allowedDigitsForP1Guess.size() > 3){
            Log.i("MainReset", "Entering 3 with allowedLength1 " + allowedDigitsForP1Guess.size());
            temp = generateRandomNumber(allowedDigitsForP1Guess);
            res = arrayToInteger(temp);
            Integer resObj = new Integer(res);
            if(!guessNumbersByP1.contains(resObj)){
                guessNumbersByP1.add(resObj);
                noRepetetion = false;
            }
        }
        return res;
    }

    // Array to int reusable conversion function
    public int arrayToInteger(int[] temp){
        int res = 0;
        for(int i = 0; i < 4; i++)
            res = res*10 + temp[i];
        return res;
    }

    // P2's guessing strategy
    public int makeAGuess2(){

        int[] temp;
        boolean noRepetetion = true;
        int res = 0;
        if(resultP2 != null && resultP2[3] == 999){
            allowedDigitsForP2Guess.clear();
            allowedDigitsForP2Guess.add(resultP2[0]%10);
            allowedDigitsForP2Guess.add((resultP2[0]/10)%10);
            allowedDigitsForP2Guess.add((resultP2[0]/100)%10);
            allowedDigitsForP2Guess.add((resultP2[0]/1000)%10);
            while(noRepetetion){
                Collections.shuffle(allowedDigitsForP2Guess);
                Log.i("MainReset", "Entering 1 with allowedLength2 " + allowedDigitsForP2Guess.get(0) + " " + allowedDigitsForP2Guess.get(1) + " " + allowedDigitsForP2Guess.get(2) + " " + allowedDigitsForP2Guess.get(3));
                temp = new int[]{allowedDigitsForP2Guess.get(0), allowedDigitsForP2Guess.get(1), allowedDigitsForP2Guess.get(2), allowedDigitsForP2Guess.get(3)};
                res = arrayToInteger(temp);
                Integer dummy = new Integer(res);
                if(!guessNumbersByP2.contains(dummy)){
                    guessNumbersByP2.add(dummy);
                    noRepetetion = false;
                    return res;
                }
            }
        }
        if(resultP2 != null && allowedDigitsForP2Guess.contains(resultP2[3])){
            Log.i("MainReset", "Entering 2 with allowedLength2 " + allowedDigitsForP2Guess.get(0) + " " + allowedDigitsForP2Guess.get(1) + " " + allowedDigitsForP2Guess.get(2) + " " + allowedDigitsForP2Guess.get(3));
            allowedDigitsForP2Guess.remove(new Integer(resultP2[3]));
        }

        while(noRepetetion && allowedDigitsForP2Guess.size() > 3){
            Log.i("MainReset", "Entering 3 with allowedLength2 " + allowedDigitsForP2Guess.size());
            temp = new int[]{allowedDigitsForP2Guess.get(0), allowedDigitsForP2Guess.get(1), allowedDigitsForP2Guess.get(2), allowedDigitsForP2Guess.get(3)};
            res = arrayToInteger(temp);
            Integer dummy = new Integer(res);
            if(!guessNumbersByP2.contains(dummy)){
                guessNumbersByP2.add(dummy);
                noRepetetion = false;
            }
        }
        return res;
    }

    // Random number generation logic
    public int[] generateRandomNumber(List<Integer> allDigitsList){
        Collections.shuffle(allDigitsList);
        Log.i("MainReset", " " + allDigitsList.get(0) + " " + allDigitsList.get(1) + " " + allDigitsList.get(2) + " " + allDigitsList.get(3));
        return new int[]{allDigitsList.get(0), allDigitsList.get(1), allDigitsList.get(2), allDigitsList.get(3)};
    }

    // Display guess number
    public int displayGeneratedNumber(TextView tv, int[] number)  {
        int temp = 0;
        for(int i = 0; i < 4; i++)
            temp = temp * 10 + number[i];
        Log.i("logs", String.valueOf(temp));
        tv.setText(String.valueOf(number[0]) + String.valueOf(number[1]) + String.valueOf(number[2]) + String.valueOf(number[3]));
        return temp;
    }
}