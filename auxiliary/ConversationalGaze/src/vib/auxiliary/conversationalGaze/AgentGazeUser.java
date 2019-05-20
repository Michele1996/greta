/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vib.auxiliary.conversationalGaze;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import vib.auxiliary.ssi.SSIFrame;
import vib.auxiliary.ssi.SSIFramePerfomer;
import vib.auxiliary.ssi.SSITypes;
import vib.core.feedbacks.Callback;
import vib.core.feedbacks.FeedbackPerformer;
import vib.core.signals.GazeSignal;
import vib.core.signals.Signal;
import vib.core.signals.SignalEmitter;
import vib.core.signals.SignalPerformer;
import vib.core.signals.SpeechSignal;
import vib.core.util.CharacterDependent;
import vib.core.util.CharacterManager;
import vib.core.util.Mode;
import vib.core.util.enums.CompositionType;
import vib.core.util.enums.GazeDirection;
import vib.core.util.enums.Influence;
import vib.core.util.environment.Animatable;
import vib.core.util.environment.Environment;
import vib.core.util.environment.Node;
import vib.core.util.id.ID;
import vib.core.util.id.IDProvider;
import vib.core.util.math.Vec3d;
import vib.core.util.time.Temporizable;
import vib.core.util.time.TimeMarker;
import vib.core.util.time.Timer;

/**
 *
 * @author Donatella Simonetti
 */
public class AgentGazeUser implements SSIFramePerfomer, SignalEmitter, SignalPerformer, CharacterDependent, FeedbackPerformer{

    private List<SignalPerformer> signalPerformers = new ArrayList<SignalPerformer>();
    private CharacterManager characterManager;
    public Environment envi;
    
    private ConversationParticipant agent;
    private ConversationParticipant user;
    
    // User head positions
    public double head_pos_x = 0;
    public double head_pos_y = 0;
    public double head_pos_z = 0;
    
    public double head_rx = 0;
    public double head_ry = 0;
    public double head_rz = 0;
    
    // cam position
    public double cam_px = 0.0;
    public double cam_py = 0.0;
    public double cam_pz = 0.0;
    
    public double cam_rx = 0.0;
    public double cam_ry = 0.0;
    public double cam_rz = 0.0;
    
    // last gazeSignal
    private GazeSignal last_gs;
            
    // vector were we store if the User is looking to the agent(0) or not(1) 
    // 0 --> mutual look
    // 1 --> look away
    private int[] vecGazeState = new int[6]; 
    private int counter = 0;
    
    private double t_bothMG; // time before breaking the mutual gaze
    private double t_bothAway; // fixed term --> afetr how much the Agent Try to initiate the eye contact 
    
    // for the agent
    private double startMG; // time start to look the user
    private double startLA;// time start to look away
    
    //
    private double startBothMG; // time start to look at the user
    private double startBothLA;// time start to look away
    
    private int[] status_AU = new int[2]; ; // AU = AgentUser
    private int[] oldStatus_AU = new int[2];
    private double startStatus_AU;
    
    private List<GazeDirection> listGazeDirection;
    
    private Thread sendGazeSignal;
    private boolean isGazing = true;
    private static final long SLEEP_TIME = 50; //miliiseconds
    private LinkedList<Signal> currentAgentSignals = new LinkedList<Signal>();;
    
    public AgentGazeUser(CharacterManager cm){  
        
        setCharacterManager(cm);
        envi = characterManager.getEnvironment();
        
        this.agent = new ConversationParticipant(characterManager);
        
        // initialize the time (msec) to look away and look mutual
        this.agent.setTime_MG(4000);
        this.agent.setTime_LA(2000);
        this.agent.setIsTalking(false);
        this.agent.setGazeStatus(1);
        this.agent.setOldGazeStatus(1);
        
        this.user = new ConversationParticipant("user");
        // if mean vecGazeState >= 0,66 --> mutual_gaze else look_away
        this.user.setGazeStatus(0);  // initialize gaze state at look away
        this.user.setOldGazeStatus(0);       
        
        // create a node user where can we send and store the position/orientation of user head
        Node check = envi.getNode("user");
        if (check == null){
            Animatable user = new Animatable();
            user.setIdentifier("user");
            envi.addNode(user);
        }
        
        // initialize the gaze for the agent
        this.last_gs = new GazeSignal("gaze");

        //
        this.t_bothAway = 1500;
        this.t_bothMG = 3000;

        this.startLA = vib.core.util.time.Timer.getTime();
        
        // initilize status AU
        this.status_AU[0] = 1;
        this.status_AU[1] = 1;
        
        this.oldStatus_AU = this.status_AU;
        //
        this.listGazeDirection = new ArrayList<GazeDirection>();
        this.listGazeDirection.add(GazeDirection.FRONT);
        this.listGazeDirection.add(GazeDirection.UP);
        this.listGazeDirection.add(GazeDirection.UPRIGHT);
        this.listGazeDirection.add(GazeDirection.DOWN);
        this.listGazeDirection.add(GazeDirection.LEFT);
        this.listGazeDirection.add(GazeDirection.RIGHT);
        this.listGazeDirection.add(GazeDirection.DOWNLEFT);
        this.listGazeDirection.add(GazeDirection.UPLEFT);
        this.listGazeDirection.add(GazeDirection.DOWNRIGHT);
        
        startListening();
    }

    private void startListening(){
        
        sendGazeSignal = new Thread(new Runnable() {
            @Override
            public void run() {
                while(isGazing){
                    listen();
                    Timer.sleep(SLEEP_TIME);
                }
            }
        });
        sendGazeSignal.setDaemon(true);
        sendGazeSignal.start();
    }
    
    // if there are gazeSignals
    public void listen() {
        CopyOnWriteArrayList<Signal> list = new CopyOnWriteArrayList<Signal>();
        list.addAll(agent.getGzSignals());
        //list =(ArrayList) Collections.synchronizedList(list);
        synchronized (list){
            ListIterator<Signal> pending = list.listIterator();
            while (pending.hasNext()) {
                Signal aSignal = pending.next();
                synchronized (currentAgentSignals){
                    currentAgentSignals.add(aSignal);
                }
            } 
        }
        
        if (!currentAgentSignals.isEmpty() ) {
            String request = "SSI"; //to change
            ID id = IDProvider.createID(request);
            this.performSignals(currentAgentSignals, id, new Mode(CompositionType.blend));
        }
        
        currentAgentSignals.clear();
        list.clear();
        agent.getGzSignals().clear();
        
        //clean the current inputs signals
        synchronized (currentAgentSignals){
            ListIterator<Signal> current = currentAgentSignals.listIterator();
            while (current.hasNext()) {
                Signal aSignal = current.next();
                double end = aSignal.getEnd().getValue();
                if (Timer.getTime() > end) {
                    current.remove();
                }
            }
        }
    }
    
    @Override
    public void performSSIFrame(SSIFrame ssi_frame, ID requestId) {

        double currentTime = vib.core.util.time.Timer.getTimeMillis();
        
        // gazeSignal to sent to the Realizer
        ArrayList<Signal> toSend = new ArrayList<Signal>();
        
        // take the head position from the xml message
        head_pos_x = ssi_frame.getDoubleValue(SSITypes.SSIFeatureNames.head_position_x);
        head_pos_y = ssi_frame.getDoubleValue(SSITypes.SSIFeatureNames.head_position_y);
        head_pos_z = ssi_frame.getDoubleValue(SSITypes.SSIFeatureNames.head_position_z);
        
        head_rx = ssi_frame.getDoubleValue(SSITypes.SSIFeatureNames.head_orientation_pitch);
        head_ry = ssi_frame.getDoubleValue(SSITypes.SSIFeatureNames.head_orientation_yaw);
        head_rz = ssi_frame.getDoubleValue(SSITypes.SSIFeatureNames.head_orientation_roll);
        
        // update the head position according the camera setting
        //cam position
        if (cam_px!=0 || cam_py!=0 || cam_pz!=0){
            head_pos_x += cam_px;
            head_pos_y += cam_py;
            head_pos_z += cam_pz;
        }
        // rotate around horizontal (x for cam), i.e. z axis of GRETA
        if (cam_rx != 0.0){
            double h_pos_x = head_pos_x*Math.cos(cam_rx) + Math.sin(cam_rx)*(head_pos_z);
            double h_pos_z = -head_pos_x*Math.sin(cam_rx) + Math.cos(cam_rx)*(head_pos_z);
            
            head_rx += - Math.toDegrees(cam_rx);
            head_pos_x = h_pos_x;
            head_pos_z = h_pos_z; 
        }
        // rotate around vertical (y for cam), i.e. y axis of GRETA
        if (cam_ry != 0.0){
            double h_pos_y = head_pos_y*Math.cos(cam_ry) - Math.sin(cam_ry)*(head_pos_z);
            double h_pos_z = head_pos_y*Math.sin(cam_ry) + Math.cos(cam_ry)*(head_pos_z);
            
            head_ry += - Math.toDegrees(cam_ry);
            head_pos_y = h_pos_y;
            head_pos_z = h_pos_z; 
        }
         // rotate around depth axis(z for cam), i.e. x axis of GRETA
        if (cam_rz != 0.0){
            double h_pos_x = head_pos_x*Math.cos(cam_rz) - Math.sin(cam_rz)*head_pos_y;
            double h_pos_y = head_pos_x*Math.sin(cam_rz) + Math.cos(cam_rz)*head_pos_y;
            
            head_rz += - Math.toDegrees(cam_rz);
            head_pos_x = h_pos_x;
            head_pos_y = h_pos_y; 
        }
        //TODO ???
        // set the first gaze for the agent
        // maybe a model that make the agent gaze randomly at the environment
        
        
        // check that the user is looking at certain region of the screen = agent face 
        // if the user is looking at agent face the state is 0
        // otherway state = 1 (look away)
        int currentUserState = 0; // to be changed 
        
        // normalize the head position
        double posX_norm = head_pos_x/Math.sqrt(Math.pow(head_pos_x, 2)+Math.pow(head_pos_z, 2));
        double posY_norm = head_pos_y/Math.sqrt(Math.pow(head_pos_y, 2)+Math.pow(head_pos_z, 2));
        // check the user is looking at the agent face
        double ratio1 = posX_norm/Math.sin(head_ry);
        double ratio2 = posY_norm/Math.sin(head_rx);
        
        //System.out.println(ratio1 + "  " +  ratio2);
        // compute currentUserState
        if(Math.abs(ratio1) > 0.3 || Math.abs(ratio2) > 2.0){ // look away
            currentUserState = 1;
        }else{// look at the agent
            currentUserState = 0;
        }
        
        // chek if the user is talking 
        // TODO: voice eyesweb
        
        // check that 6 frames are passed
        if (counter < 5){
            vecGazeState[counter] = currentUserState;
            counter += 1;        
        }else{ // check each 6 frames if generate the gaze for the agent 
            vecGazeState[counter] = currentUserState;
            //System.out.println(vecGazeState.toString());
            double sumW = Arrays.stream(vecGazeState).sum();
            if ((sumW/vecGazeState.length) >= 0.66){
                this.user.setGazeStatus(1);
            }else{
                 this.user.setGazeStatus(0);
            }
            
            // TODO: consider who is speaking 
            // according to talking or listening state the duration of mutual gaze or gaze away can change. The longe mutual gaze for the listener than the Speaker
            
            status_AU[0] = getAgent().getGazeStatus();
            status_AU[1] = this.user.getGazeStatus();
            if (status_AU != oldStatus_AU){
                startStatus_AU =  vib.core.util.time.Timer.getTimeMillis(); // starting time of each status
            }
            //System.out.println(status_AU + " " + currentTime);
            //create the agent gaze according to the status (A,U)
            if (status_AU[0] == 1 && status_AU[1] == 0){
                if ((currentTime - startLA) > getAgent().getTime_LA()){ 
                    getAgent().setGazeStatus(0); // the aversion last too much so the agent gaze at the user
                    status_AU[0] = 0;
                    startMG = vib.core.util.time.Timer.getTimeMillis();
                    GazeSignal gs = createGazeSignal(getAgent().getGazeStatus(), head_pos_x, head_pos_y, head_pos_z);
                    toSend.add(gs);
                }
            }else if(status_AU[0] == 1 && status_AU[1] == 1){
                if ((currentTime - startStatus_AU) > getT_bothAway()){ 
                    getAgent().setGazeStatus(0); 
                    status_AU[0] = 0;
                    startMG = vib.core.util.time.Timer.getTimeMillis();
                    GazeSignal gs = createGazeSignal(getAgent().getGazeStatus(), head_pos_x, head_pos_y, head_pos_z);
                    toSend.add(gs);
                }
            }else if(status_AU[0] == 0 && status_AU[1] == 0){
                if ((currentTime - startStatus_AU) > getT_bothMG()){ 
                    getAgent().setGazeStatus(1); 
                    status_AU[0] = 1;
                    GazeSignal gs = createGazeSignal(getAgent().getGazeStatus(), head_pos_x, head_pos_y, head_pos_z);
                    toSend.add(gs);
                }
            }else{
                if ((currentTime - startMG) > getAgent().getTime_MG()){ 
                    getAgent().setGazeStatus(1); 
                    status_AU[0] = 1;
                    GazeSignal gs = createGazeSignal(getAgent().getGazeStatus(), head_pos_x, head_pos_y, head_pos_z);
                    toSend.add(gs);
                }
            }
            counter = 0;            
        }
        
        // update
        getAgent().setOldGazeStatus(getAgent().getGazeStatus());
        this.user.setOldGazeStatus(this.user.getGazeStatus());
        oldStatus_AU = status_AU;
        
        
        // update the position of the user in the environment
        Animatable us = (Animatable) envi.getNode("user");
        // the eyesweb give a coordination system different from what we have in Greta
        // eyw-->Grata: x-->-z, z-->x
        us.setCoordinates(new Vec3d(1+head_pos_z,head_pos_y,-head_pos_x));
        //us.setOrientation(0, 0, 0); // send also the orientation
        
        this.performSignals(toSend, requestId, new Mode(CompositionType.blend));
    }
    
    public GazeSignal createGazeSignal(int agentGazeStatus, double head_pos_x, double head_pos_y, double head_pos_z){
        
        // create gaze signal
        GazeSignal gs = new GazeSignal("gaze");
        

            gs.setGazeShift(true);
            //dummy time values
            gs.getTimeMarker("start").setValue(0.0);
            gs.getTimeMarker("end").setValue(0.4);

            gs.setTarget("user");           

        if (agentGazeStatus == 0){ // look at the agent    
            //radius to normalize the position and compute the angle via acos
            double radius_yaw = Math.sqrt(Math.pow(head_pos_z, 2) + Math.pow(head_pos_x, 2));
            double radius_pitch = Math.sqrt(Math.pow(head_pos_y, 2) + Math.pow(head_pos_z, 2));            
            double z = Math.abs(head_pos_z)/radius_yaw;
            double y = Math.abs(head_pos_y)/radius_pitch;
            
        
            // set the gaze influence
            if (Math.toDegrees(Math.acos(y)) > 15 || Math.toDegrees(Math.acos(z)) > 25){
                gs.setInfluence(Influence.HEAD);
            }else if(Math.toDegrees(Math.acos(z)) > 40){
                gs.setInfluence(Influence.TORSO);
            }else{
                gs.setInfluence(Influence.EYES);
            } 
        }else { // look away
            /*gs.setGazeShift(true);
            //gs.setTarget("");   
            gs.getTimeMarker("start").setValue(0.0);
            gs.getTimeMarker("end").setValue(0.4);*/
            
            // take randomly a direction 
            int max = 8; // total number of GazeDirection
            int min = 1; 
            Random rn = new Random();
            int randomDirection = rn.nextInt(max - min + 1) + min;
            
            gs.setOffsetDirection(listGazeDirection.get(randomDirection));
            // shift angle set to 30
            gs.setOffsetAngle(15); // we move also the head 
            gs.setInfluence(Influence.EYES);
        }
        //}
        return gs;
    }
    
    @Override
    public void addSignalPerformer(SignalPerformer performer) {
        signalPerformers.add(performer);
    }

    @Override
    public void removeSignalPerformer(SignalPerformer performer) {
        signalPerformers.add(performer);
    }
    
    @Override
    public void onCharacterChanged() {
        // 
    }

    @Override
    public CharacterManager getCharacterManager() {
        if(characterManager==null)
            characterManager = CharacterManager.getStaticInstance();
        return characterManager;
    }

    @Override
    public void setCharacterManager(CharacterManager characterManager) {
        this.characterManager = characterManager;
    }

    @Override
    public void performSSIFrames(List<SSIFrame> list, ID id) {  
         for (SSIFrame ssf : list) {
            performSSIFrame(ssf, id);
        }
    }
    
    @Override
    public void performSignals(List<Signal> list, ID id, Mode mode) {
        // send the Gaze Signals to Realizer
        for(SignalPerformer sp : signalPerformers){
            sp.performSignals(list, id, mode);
        }
    }

    @Override
    public void performFeedback(ID id, String string, List<Temporizable> list) {
          // info about the signals    
    }

    @Override
    public void performFeedback(Callback clbck) { 
        if (clbck.type() == "start"){
            this.agent.setIsTalking(true);
        }else if(clbck.type() == "end"){
            this.agent.setIsTalking(false);
        }
    }

    @Override
    public void setDetailsOption(boolean bln) {
        
    }

    @Override
    public boolean areDetailedFeedbacks() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDetailsOnFace(boolean bln) {
        
    }

    @Override
    public boolean areDetailsOnFace() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setDetailsOnGestures(boolean bln) {
        
    }

    @Override
    public boolean areDetailsOnGestures() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
        /**
     * @return the agent
     */
    public ConversationParticipant getAgent() {
        return agent;
    }

    /**
     * @param agent the agent to set
     */
    public void setAgent(ConversationParticipant agent) {
        this.agent = agent;
    }

    /**
     * @return the t_bothMG
     */
    public double getT_bothMG() {
        return t_bothMG;
    }

    /**
     * @param t_bothMG the t_bothMG to set
     */
    public void setT_bothMG(double t_bothMG) {
        this.t_bothMG = t_bothMG;
    }

    /**
     * @return the t_bothAway
     */
    public double getT_bothAway() {
        return t_bothAway;
    }

    /**
     * @param t_bothAway the t_bothAway to set
     */
    public void setT_bothAway(double t_bothAway) {
        this.t_bothAway = t_bothAway;
    }

    @Override
    public void performFeedback(ID id, String string, SpeechSignal ss, TimeMarker tm) {
        // info about the signals
    }

}
