import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.*;

public class Scheduler{
    public static final int ARRIVAL = 0;
    public static final int UNBLOCK = 1;
    public static final int TIMER = 2;
    
    private static final DecimalFormat df = new DecimalFormat("0.00");
    
    public static class Stats{
        int serviceTime = 0;
        int startTime = 0;
        int finishTime = 0;
        int lastReady = 0;
        int totalResponseTime = 0;
        int numResponseTime = 0;
        double spnServiceTime = 0;
    }
    
    public static class Process{
        Stats stats = new Stats();
        int pid;
        int arrivalTime;
        int turnaroundTime;
        int normalizedTurnaroundTime;
        int avgResponseTime;
        double placeholder;
        boolean spn;
        ArrayList <Integer> activities;
        
        public Process(int p, int at, ArrayList <Integer> act){
            pid = p;
            arrivalTime = at;
            activities = act;
        }
        
        public void stats(){
            turnaroundTime = (stats.finishTime - arrivalTime);
            normalizedTurnaroundTime = ((stats.finishTime - arrivalTime) / stats.serviceTime);
            avgResponseTime = stats.totalResponseTime / stats.numResponseTime;
            
            System.out.println("Process " + pid);
            System.out.println("    Start Time: " + stats.startTime);
            System.out.println("    Finish Time: " + stats.finishTime);
            System.out.println("    Service Time: " + stats.serviceTime);
            if(spn)
                System.out.println("    Predicted Service Time: " + df.format(stats.spnServiceTime));
            System.out.println("    Turnaround Time: " + turnaroundTime);
            System.out.println("    Normalized Turnaround Time: " + normalizedTurnaroundTime);
            System.out.println("    Average Time: " + avgResponseTime);
        }
    }
    
    public static class Event implements Comparable <Event>{
        int pid;
        int type;
        int time;
        
        public Event(int etype, int ppid, int ttime){
            pid = ppid;
            type = etype;
            time = ttime;
        }
        
        @Override
        public int compareTo(Event o){
            return Integer.compare(this.time, o.time);
        }
    }
    
    public static class SchedulerFile{
        String scheduler = null;
        String key1 = null;
        String key2 = null;
        
        public void readSF(String schedulerFile) throws FileNotFoundException{
            File file = new File(schedulerFile);
            Scanner in = new Scanner(file);
            while(in.hasNextLine()){
                scheduler = in.nextLine();
                
                if(in.hasNextLine()){
                    String[] holder = in.nextLine().split("=");
                    key1 = holder[1];
                    
                    if(in.hasNextLine()){
                        String[] holder2 = in.nextLine().split("=");
                        key2 = holder2[1];
                    } else
                        break;
                } else
                    break;
            }
        }
    }
    
    public static class ProcessFile{
        ArrayList <Process> procs = new ArrayList <>();
        
        public void readPF(String processFile) throws FileNotFoundException{
            int i = 0;
            File file = new File(processFile);
            Scanner in = new Scanner(file);
            
            while(in.hasNextLine()){
                String s = in.nextLine();
                String[] holder = s.split("\\s+");
                ArrayList <Integer> n = new ArrayList <>();
                
                for(int y = 1; y < holder.length; y++){
                    n.add(Integer.parseInt(holder[y]));
                }
                
                procs.add(new Process(i, Integer.parseInt(holder[0]), n));
                i++;
            }
        }
    }
    
    public static class FCFS{
        ArrayList <Process> procsFCFS;
        ArrayList <Process> queue = new ArrayList <>();
        PriorityQueue <Event> eventQ = new PriorityQueue <>();
        Process currentRunning = null;
        int clock = 0;
        int runningTime = 0;
        int nextEvent = 0;
        
        public FCFS(ArrayList <Process> p){
            procsFCFS = p;
            for(Process x : procsFCFS)
                for(int n = 0; n < x.activities.size(); n += 2)
                    x.stats.serviceTime += x.activities.get(n);
        }
        
        public void simTest(){
            for(Process x : procsFCFS){
                eventQ.add(new Event(ARRIVAL, x.pid, x.arrivalTime));
            }
            
            while(! eventQ.isEmpty() || currentRunning != null){
                if(eventQ.peek() != null)
                    nextEvent = eventQ.peek().time;
                else nextEvent = - 1;
                
                if(currentRunning != null && (nextEvent == - 1 || nextEvent > runningTime + clock)){
                    clock = clock + runningTime;
                    runningTime = 0;
                } else{
                    if(currentRunning != null)
                        runningTime -= nextEvent - clock;
                    clock = nextEvent;
                }
                if(currentRunning != null && runningTime == 0){
                    if(currentRunning.activities.isEmpty())
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is exiting");
                        currentRunning.stats.finishTime = clock;
                    else{
                        int time = currentRunning.activities.remove(0);
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is blocking for " + time + " time units");
                        eventQ.add(new Event(UNBLOCK, currentRunning.pid, clock + time));
                    }
                    currentRunning = null;
                }
                
                while(! eventQ.isEmpty() && eventQ.peek().time == clock){
                    Event e = eventQ.remove();
                    Process pl = procsFCFS.get(e.pid);
                    if(e.type == ARRIVAL){
                        //System.out.println("[" + clock + "] Process " + pl.pid + " has arrived");
                        queue.add(pl);
                        pl.stats.lastReady = clock;
                    } else{
                        //System.out.println("[" + clock + "] Process " + pl.pid + " unblocks");
                        queue.add(pl);
                        pl.stats.lastReady = clock;
                    }
                }
                
                if(currentRunning == null && ! queue.isEmpty()){
                    //System.out.print("[" + clock + "] Current Ready Queue ");
                    //for(Process p : queue)
                    //System.out.print(p.activities);
                    //System.out.println();
                    Process l = queue.remove(0);
                    l.spn = false;
                    l.stats.totalResponseTime += clock - l.stats.lastReady;
                    l.stats.numResponseTime++;
                    int cpuTime = l.activities.remove(0);
                    if(l.stats.startTime == 0)
                        l.stats.startTime = clock;
                    runningTime = cpuTime;
                    currentRunning = l;
                    //System.out.println("[" + clock + "] Dispatching process " + l.pid);
                }
            }
        }
    }
    
    public static class RR{
        ArrayList <Process> procsRR;
        ArrayList <Process> queue = new ArrayList <>();
        PriorityQueue <Event> eventQ = new PriorityQueue <>();
        
        int timeQuantum;
        int clock = 0;
        int runningTime = 0;
        int nextEvent = 0;
        Process currentRunning;
        public RR(ArrayList <Process> p, int qt){
            procsRR = p;
            timeQuantum = qt;
            for(Process x : procsRR)
                for(int n = 0; n < x.activities.size(); n += 2)
                    x.stats.serviceTime += x.activities.get(n);
        }
        
        public void simTest(){
            for(Process x : procsRR)
                eventQ.add(new Event(ARRIVAL, x.pid, x.arrivalTime));
            
            while(! eventQ.isEmpty() || currentRunning != null){
                if(eventQ.peek() != null){
                    nextEvent = eventQ.peek().time;
                } else nextEvent = - 1;
                
                if(runningTime > timeQuantum){
                    runningTime = timeQuantum;
                }
                
                if(currentRunning != null && (nextEvent == - 1 || nextEvent > runningTime + clock)){
                    clock = clock + runningTime;
                    runningTime = 0;
                } else{
                    if(currentRunning != null)
                        runningTime -= nextEvent - clock;
                    clock = nextEvent;
                }
                
                if(currentRunning != null && runningTime == 0){
                    if(currentRunning.activities.isEmpty()){
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is exiting");
                        currentRunning.stats.finishTime = clock;
                    } else if(currentRunning.activities.size() % 2 == 0){
                        int time = currentRunning.activities.remove(0);
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is blocking for " + time + " time units");
                        eventQ.add(new Event(UNBLOCK, currentRunning.pid, clock + time));
                    } else{
                        currentRunning.activities.set(0, currentRunning.activities.get(0) - timeQuantum);
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " timed out!");
                        eventQ.add(new Event(TIMER, currentRunning.pid, clock));
                    }
                    currentRunning = null;
                }
                
                while(! eventQ.isEmpty() && eventQ.peek().time == clock){
                    Event e = eventQ.remove();
                    Process pl = procsRR.get(e.pid);
                    if(e.type == ARRIVAL){
                        //System.out.println("[" + clock + "] Process " + pl.pid + " has arrived");
                        queue.add(pl);
                        pl.stats.lastReady = clock;
                    } else if(e.type == TIMER){
                        queue.add(pl);
                        pl.stats.lastReady = clock;
                    } else{
                        //System.out.println("[" + clock + "] Process " + pl.pid + " unblocks");
                        queue.add(pl);
                        pl.stats.lastReady = clock;
                    }
                }
                
                if(currentRunning == null && ! queue.isEmpty()){
//                    System.out.print("[" + clock + "] Current Ready Queue ");
//                    for(Process p : queue)
//                        System.out.print("Process " + p.pid + " " + p.activities + " ");
//                    System.out.println();
                    Process l = queue.remove(0);
                    l.spn = false;
                    l.stats.totalResponseTime += clock - l.stats.lastReady;
                    l.stats.numResponseTime++;
                    int cpuTime = l.activities.get(0);
                    if(cpuTime < timeQuantum){
                        cpuTime = l.activities.remove(0);
                    }
                    if(l.stats.startTime == 0)
                        l.stats.startTime = clock;
                    runningTime = cpuTime;
                    currentRunning = l;
                    //System.out.println("[" + clock + "] Dispatching process " + l.pid);
                }
            }
        }
    }
    
    public static class SPN{
        ArrayList <Process> procsSPN;
        ArrayList <Process> queue = new ArrayList <>();
        PriorityQueue <Event> eventQ = new PriorityQueue <>();
        
        boolean service_given;
        double alpha = 0;
        int clock = 0;
        int runningTime = 0;
        int nextEvent = 0;
        Process currentRunning;
        public SPN(ArrayList <Process> p, String qt, double al){
            procsSPN = p;
            alpha = al;
            service_given = Boolean.parseBoolean(qt);
        }
        
        public void simTest(){
            for(Process x : procsSPN){
                eventQ.add(new Event(ARRIVAL, x.pid, x.arrivalTime));
            }
            
            while(! eventQ.isEmpty() || currentRunning != null){
                if(eventQ.peek() != null)
                    nextEvent = eventQ.peek().time;
                else nextEvent = - 1;
                
                if(currentRunning != null && (nextEvent == - 1 || nextEvent > runningTime + clock)){
                    clock = clock + runningTime;
                    runningTime = 0;
                } else{
                    if(currentRunning != null)
                        runningTime -= nextEvent - clock;
                    clock = nextEvent;
                }
                if(currentRunning != null && runningTime == 0){
                    if(currentRunning.activities.isEmpty()){
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is exiting");
                        currentRunning.stats.finishTime = clock;
                    } else{
                        int time = currentRunning.activities.remove(0);
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is blocking for " + time + " time units");
                        eventQ.add(new Event(UNBLOCK, currentRunning.pid, clock + time));
                    }
                    currentRunning = null;
                }
                
                while(! eventQ.isEmpty() && eventQ.peek().time == clock){
                    Event e = eventQ.remove();
                    Process pl = procsSPN.get(e.pid);
                    if(e.type == ARRIVAL){
                        //System.out.println("[" + clock + "] Process " + pl.pid + " has arrived");
                        queue.add(pl);
                        pl.stats.lastReady = clock;
                    } else{
                        //System.out.println("[" + clock + "] Process " + pl.pid + " unblocks");
                        queue.add(pl);
                        pl.stats.lastReady = clock;
                    }
                }
                
                if(currentRunning == null && ! queue.isEmpty()){
//                    System.out.print("[" + clock + "] Current Ready Queue ");
//                    for(Process p : queue)
//                        System.out.print(p.activities);
//                    System.out.println();
                    
                    queue.sort(Comparator.comparingInt(p -> p.activities.get(0)));
                    Process l = queue.remove(0);
                    l.spn = true;
                    int responseTime = clock - l.stats.lastReady;
                    l.stats.totalResponseTime += responseTime;
                    l.stats.numResponseTime++;
                    int cpuTime = l.activities.remove(0);
                    
                    //This predicts the service time using either the first CPU process or
                    //The exponential average method where the first CPU process is the initial activity
                    if(service_given)
                        l.stats.spnServiceTime = l.activities.get(0);
                    else{
                        if(l.placeholder == 0){
                            l.placeholder += (alpha * cpuTime + (1 - alpha) * l.activities.get(0));
                            l.stats.spnServiceTime = l.placeholder;
                        } else{
                            double oldAvg = l.placeholder;
                            l.placeholder += (alpha * cpuTime + (1 - alpha) * oldAvg);
                            l.stats.spnServiceTime = l.placeholder;
                        }
                    }
                    
                    if(l.stats.startTime == 0){
                        l.stats.startTime = clock;
                    }
                    runningTime = cpuTime;
                    l.stats.serviceTime += cpuTime;
                    currentRunning = l;
                    //System.out.println("[" + clock + "] Dispatching process " + l.pid);
                }
            }
        }
    }
    
    public static class FEEDBACK{
        ArrayList <Process> procsFB;
        ArrayList <Process> queue = new ArrayList <>();
        PriorityQueue <Event> eventQ = new PriorityQueue <>();
        
        int numPriorities = 0;
        int clock = 0;
        int runningTime = 0;
        int nextEvent = 0;
        int qnum = 0;
        ArrayList <ArrayList <Process>> priorityQs;
        int timeQuantum = 0;
        Process currentRunning;
        public FEEDBACK(ArrayList <Process> p, int qt, int al){
            procsFB = p;
            timeQuantum = qt;
            numPriorities = al;
            
            priorityQs = new ArrayList <>();
            for(int i = 0; i < numPriorities; i++){
                priorityQs.add(new ArrayList <>());
            }
            
            for(Process x : procsFB)
                for(int n = 0; n < x.activities.size(); n += 2)
                    x.stats.serviceTime += x.activities.get(n);
        }
        
        public void simTest(){
            for(Process x : procsFB)
                eventQ.add(new Event(ARRIVAL, x.pid, x.arrivalTime));
            
            while(! eventQ.isEmpty() || currentRunning != null){
                if(eventQ.peek() != null){
                    nextEvent = eventQ.peek().time;
                } else nextEvent = - 1;
                
                if(runningTime > timeQuantum){
                    runningTime = timeQuantum;
                }
                
                if(currentRunning != null && (nextEvent == - 1 || nextEvent > runningTime + clock)){
                    clock = clock + runningTime;
                    runningTime = 0;
                } else{
                    if(currentRunning != null)
                        runningTime -= nextEvent - clock;
                    clock = nextEvent;
                }
                
                if(currentRunning != null && runningTime == 0){
                    if(currentRunning.activities.isEmpty()){
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is exiting");
                        currentRunning.stats.finishTime = clock;
                    } else if(currentRunning.activities.size() % 2 == 0){
                        int time = currentRunning.activities.remove(0);
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " is blocking for " + time + " time units");
                        eventQ.add(new Event(UNBLOCK, currentRunning.pid, clock + time));
                    } else{
                        currentRunning.activities.set(0, currentRunning.activities.get(0) - timeQuantum);
                        //System.out.println("[" + clock + "] Process " + currentRunning.pid + " timed out!");
                        eventQ.add(new Event(TIMER, currentRunning.pid, clock));
                    }
                    currentRunning = null;
                }
                
                while(! eventQ.isEmpty() && eventQ.peek().time == clock){
                    Event e = eventQ.remove();
                    Process pl = procsFB.get(e.pid);
                    if(e.type == ARRIVAL){
                        //System.out.println("[" + clock + "] Process " + pl.pid + " has arrived");
                        priorityQs.get(0).add(pl);
                        pl.stats.lastReady = clock;
                        qnum++;
                    } else if(e.type == TIMER){
                        if(qnum >= numPriorities){
                            qnum = numPriorities - 1;
                        }
                        if(qnum == (numPriorities - 1)){
                            if(! priorityQs.get(qnum).contains(pl)){
                                priorityQs.get(qnum).add(pl);
                                qnum++;
                            }
                        } else if(qnum < numPriorities - 1){
                            if(! priorityQs.get(qnum).contains(pl)){
                                priorityQs.get(qnum).add(pl);
                                qnum++;
                            }
                        }
                        pl.stats.lastReady = clock;
                    } else{
                        //System.out.println("[" + clock + "] Process " + pl.pid + " unblocks");
                        if(qnum >= numPriorities){
                            qnum = numPriorities - 1;
                        }
                        if(qnum == (numPriorities - 1)){
                            if(! priorityQs.get(qnum).contains(pl)){
                                priorityQs.get(qnum).add(pl);
                                qnum++;
                            }
                        } else if(qnum < numPriorities - 1){
                            if(! priorityQs.get(qnum).contains(pl)){
                                priorityQs.get(qnum).add(pl);
                                qnum++;
                            }
                        }
                        pl.stats.lastReady = clock;
                    }
                }
                
                if(currentRunning == null && ! priorityQs.isEmpty()){
                    //System.out.print("[" + clock + "] Current Ready Queue ");
//                    for(ArrayList <Process> x : priorityQs){
//                        System.out.print("[");
//                        for(Process l : x)
//                            System.out.print("Process " + l.pid + " " + l.activities);
//                        System.out.print("]");
//                    }
                    
                    //System.out.println();
                    for(int i = 0; i < numPriorities; i++){
                        ArrayList <Process> currentQ = priorityQs.get(i);
                        
                        if(! currentQ.isEmpty()){
                            Process l = currentQ.remove(0);
                            l.spn = false;
                            l.stats.totalResponseTime += clock - l.stats.lastReady;
                            l.stats.numResponseTime++;
                            int cpuTime = l.activities.get(0);
                            
                            if(cpuTime < timeQuantum)
                                cpuTime = l.activities.remove(0);
                            
                            if(l.stats.startTime == 0)
                                l.stats.startTime = clock;
                            runningTime = cpuTime;
                            currentRunning = l;
                            
                            //System.out.println("[" + clock + "] Dispatching Process " + l.pid + " from priority " + i);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) throws FileNotFoundException{
        int mtt = 0;
        int mntt = 0;
        int mart = 0;
        SchedulerFile sf = new SchedulerFile();
        ProcessFile pf = new ProcessFile();
        Scanner cmdRead = new Scanner(System.in);
        
        String sfIn = cmdRead.nextLine();
        String pfIn = cmdRead.nextLine();
        sf.readSF(sfIn);
        pf.readPF(pfIn);
        
        //COMMENT BEFORE SUBMITTING
//        sf.readSF("E:\\CS4348Prjs\\CS 4348 Prj 3\\algorithms\\fcfs.sf");
//        pf.readPF("E:\\CS4348Prjs\\CS 4348 Prj 3\\processes\\complex.pf");
        
        if(sf.scheduler.equalsIgnoreCase("FCFS")){
            System.out.println("___________________________________________________________");
            System.out.println("FCFS scheduler chosen");
            System.out.println("___________________________________________________________");
            FCFS sim = new FCFS(pf.procs);
            sim.simTest();
            for(Process x : pf.procs){
                x.stats();
                mtt += x.turnaroundTime;
                mntt += x.turnaroundTime / x.stats.serviceTime;
                mart += x.avgResponseTime;
            }
            System.out.println("___________________________________________________________");
            System.out.println("Mean Turnaround Time: " + mtt / pf.procs.size());
            System.out.println("Mean Normalized Turnaround Time: " + mntt / pf.procs.size());
            System.out.println("Mean Response Time: " + mart / pf.procs.size());
        }
        
        if(sf.scheduler.equalsIgnoreCase("RR")){
            System.out.println("___________________________________________________________");
            System.out.println("RR scheduler chosen");
            System.out.println("Time Quantum: " + sf.key1);
            System.out.println("___________________________________________________________");
            RR sim = new RR(pf.procs, Integer.parseInt(sf.key1));
            sim.simTest();
            for(Process x : pf.procs){
                x.stats();
                mtt += x.turnaroundTime;
                mntt += x.turnaroundTime / x.stats.serviceTime;
                mart += x.avgResponseTime;
            }
            System.out.println("___________________________________________________________");
            System.out.println("Mean Turnaround Time: " + mtt / pf.procs.size());
            System.out.println("Mean Normalized Turnaround Time: " + mntt / pf.procs.size());
            System.out.println("Mean Response Time: " + mart / pf.procs.size());
        }
        
        if(sf.scheduler.equalsIgnoreCase("SPN")){
            System.out.println("___________________________________________________________");
            System.out.println("SPN scheduler chosen");
            System.out.println("Service Given: " + sf.key1);
            System.out.println("Alpha: " + sf.key2);
            System.out.println("___________________________________________________________");
            SPN sim = new SPN(pf.procs, sf.key1, Double.parseDouble(sf.key2));
            sim.simTest();
            for(Process x : pf.procs){
                x.stats();
                mtt += x.turnaroundTime;
                mntt += x.turnaroundTime / x.stats.serviceTime;
                mart += x.avgResponseTime;
            }
            System.out.println("___________________________________________________________");
            System.out.println("Mean Turnaround Time: " + mtt / pf.procs.size());
            System.out.println("Mean Normalized Turnaround Time: " + mntt / pf.procs.size());
            System.out.println("Mean Response Time: " + mart / pf.procs.size());
        }
        
        if(sf.scheduler.equalsIgnoreCase("FEEDBACK")){
            System.out.println("___________________________________________________________");
            System.out.println("FEEDBACK scheduler chosen");
            System.out.println("Num Priorities: " + sf.key1);
            System.out.println("Quantum: " + sf.key2);
            System.out.println("___________________________________________________________");
            FEEDBACK sim = new FEEDBACK(pf.procs, Integer.parseInt(sf.key1), Integer.parseInt(sf.key2));
            sim.simTest();
            for(Process x : pf.procs){
                x.stats();
                mtt += x.turnaroundTime;
                mntt += x.turnaroundTime / x.stats.serviceTime;
                mart += x.avgResponseTime;
            }
            System.out.println("___________________________________________________________");
            System.out.println("Mean Turnaround Time: " + mtt / pf.procs.size());
            System.out.println("Mean Normalized Turnaround Time: " + mntt / pf.procs.size());
            System.out.println("Mean Response Time: " + mart / pf.procs.size());
        }
    }
}
