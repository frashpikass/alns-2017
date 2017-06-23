package alns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

/**
 * This class stores objects of type <tt>T</tt> and their weights that determine
 * their chance of being randomly chosen.
 * @author Frash
 * @param <T> the type of stored objects
 */
public class ObjectDistribution<T> {
    /**
     * Constant that holds the default value for weight
     */
    final static private Double DEFAULT_WEIGHT = 1.0;
    
    /**
     * Constant that holds the default value for the lower bound of a bin
     */
    final static private Double DEFAULT_BIN_INF = 0.0;
    
    /**
     * Constant that holds the default value for the upper bound of a bin
     */
    final static private Double DEFAULT_BIN_SUP = 0.0;
    
    /**
     * Stores the objects in a dynamic list.
     */
    private List<T> objects;
    
    /**
     * Stores the weights of the inserted objects.
     */
    private List<Double> weights;
    
    /**
     * Stores the bins associated to the weights of the inserted objects.
     */
    private List<Bin> bins;
    
    /**
     * A random generator to be used in extraction procedures. The random seed
     * is chosen when the constructor for this class is called.
     */
    private Random randomGenerator;
    
    /**
     * Constructor for an ObjectDistribution object.
     */
    public ObjectDistribution() {
        objects = new ArrayList<>();
        weights = new ArrayList<>();
        bins = new ArrayList<>();
        randomGenerator = new Random();
    }
    
    /**
     * Tries to add a new object to the distribution and updates all bins and
     * weights consequently.
     * <br>Weights are initially set to DEFAULT_WEIGHT
     * 
     * @param o the object to add
     * @return true if the distribution has changed as a consequence
     */
    public boolean add(T o){
        boolean ret = false;
        if(objects.add(o))
            if(weights.add(DEFAULT_WEIGHT))
                if(bins.add(new Bin(DEFAULT_BIN_INF, DEFAULT_BIN_SUP))){
                    ret = true;
                    this.updateBins();
                }
        return ret;
    }
    
    /**
     * Tries to add all the specified objects to the distribution and updates
     * all weights and bins consequently. This is more efficient than adding new
     * objects one by one because the bins are updated only once at the end of
     * the process (only if everything goes well).
     * <br>Weights are initially set to DEFAULT_WEIGHT
     * 
     * @param objectList the list of objects to add
     * @return true if the distribution has changed as a consequence
     */
    public boolean addAll(List<T> objectList){
        boolean ret = true;
        for(T o : objectList){
            ret = ret && objects.add(o) && weights.add(DEFAULT_WEIGHT) && bins.add(new Bin(DEFAULT_BIN_INF, DEFAULT_BIN_SUP));
        }
        if(ret){
            this.updateBins();
        }
        return ret;
    }
    
    /**
     * Tries to remove the first occurence of an object from the distribution
     * and updates all bins and weights consequently.
     * If this distribution does not contain the element, it is unchanged.
     * 
     * @param o the object to remove
     * @return true if the distribution has changed as a consequence
     */
    public boolean remove(T o){
        boolean ret = false;
        if(o != null){
            int id = objects.indexOf(o);
            int oldsize = objects.size();

            if(objects.remove(o) && id >= 0 && id < oldsize){
                weights.remove(id);
                bins.remove(id);
                ret = true;
            }

            this.updateBins();
        }
        return ret;
    }
    
    /**
     * Removes from this distribution all of its elements that are contained in the
     * specified List.
     *
     * @param c List containing elements to be removed from this distribution
     * @return <tt>true</tt> if this distribution changed as a result of the call
     */
    boolean removeAll(List<T> c){
            boolean ret = false;
            
            if(c != null && !c.isEmpty()){
            for(T o : c){
                if(o != null){
                    int id = objects.indexOf(o);
                    int oldsize = objects.size();

                    if(objects.remove(o) && id >= 0 && id < oldsize){
                        weights.remove(id);
                        bins.remove(id);
                        ret = true;
                    }
                }
            }
            this.updateBins();
        }
        return ret;
    }
    
    /**
     * Updates the weight of the specified object.
     * <br><b>IMPORTANT:</b> this method won't update the bins after the weight update!
     * To have such a behaviour use method <tt>updateWeightSafely</tt> instead.
     * @param o the object we want to update the weight of
     * @param newWeight the new weight for the specified object (if it's negative, the absolute value will be taken)
     * @return true if the weight was changed
     */
    public boolean updateWeight(T o, double newWeight){
        boolean ret = false;
        int index = this.objects.indexOf(o);
        if(index>0){
            this.weights.set(index, Math.abs(newWeight));
            ret = true;
        }
        return ret;
    }
    
    /**
     * Updates the weight of the specified object and all the bins accordingly.
     * @param o the object we want to update the weight of
     * @param newWeight the new weight for the specified object
     * @return true if the weight was changed
     */
    public boolean updateWeightSafely(T o, double newWeight){
        boolean ret = this.updateWeight(o, newWeight);
        if(ret) this.updateBins();
        return ret;
    }
    
    /**
     * Update the boundaries of all the bins in this distribution.
     * This will generate the probability distribution function for all the bins.
     */
    public void updateBins(){
        double totalWeight = this.getTotalWeight();
        double inf;
        double sup = 0.0;
        
        for(int i = 0; i<this.objects.size(); i++){
            double normalizedWeight = weights.get(i)/totalWeight;
            Bin bin = bins.get(i);
            
            // Update the bin bounds
            inf = sup;
            sup = inf + normalizedWeight;
            bin.update(inf, sup);
        }
    }
    
    /**
     * Calculates the total weight of the objects in this distribution.
     * @return the total weight of the objects in this distribution.
     */
    public double getTotalWeight(){
        return weights.stream().mapToDouble(Double::doubleValue).sum();
    }
    
    /**
     * Gets the weight of the specified object in this distribution.
     * @param o the object to retrieve the weight of
     * @return the weight of the specified object in this distribution
     */
    public double getWeightOf(T o){
        return weights.get(objects.indexOf(o));
    }
    
    /**
     * Resets all object weights to the default value and updates the bins.
     */
    public void resetWeights(){
        for(int i = 0; i<weights.size(); i++){
            weights.set(i, DEFAULT_WEIGHT);
        }
        this.updateBins();
    }
    
    /**
     * Performs a random extraction of an object in this distribution, according
     * to the probability distribution given by the object weights.
     * @return the extracted object, <tt>null</tt> if no object was extracted.
     */
    public T getRandom(){
        T ret = null;
        double r = this.randomGenerator.nextDouble();
        
        for(int i = 0; i<this.objects.size(); i++){
            if(bins.get(i).inBin(r)){
                ret = objects.get(i);
                break;
            }
        }
        
        return ret;
    }
    
    /**
     * Returns a reference to the object at the specified index position in the underlying data structure.
     * @param index the index of the object to retrieve
     * @return a reference to the object at position index in the underlying data structure
     */
    public T getReferenceFromIndex(int index){
        return this.objects.get(index);
    }
    
    @Override
    public String toString(){
        StringBuffer sb = new StringBuffer("{");
        for(int i = 0; i<this.objects.size(); i++){
            sb.append("\n\t"+bins.get(i).toString()+" -> "+objects.get(i).toString()+
                    " (weight: "+weights.get(i)+")");
        }
        sb.append("\n}");
        
        return sb.toString();
    }
    
    /**
     * Returns the first object with the highest probability of being randomly chosen.
     * @return the first object with the highest probability of being randomly chosen, null if it can't be found
     */
    public T getMostProbable(){
        T ret;
        Optional<Double> maxWeight = this.weights.stream().max(Double::compare);
        try{
            ret = this.objects.get(this.weights.indexOf(maxWeight.get()));
        }
        catch(NoSuchElementException n){
            ret = null;
        }
        return ret;
    }
    
    /**
     * Returns the list of objects with the highest probability of being randomly chosen.
     * @return the list of objects with the highest probability of being randomly chosen,
     * null if they can't be found
     */
    public List<T> getAllMostProbable(){
        List<T> ret = new ArrayList<>();
        Optional<Double> maxWeight = this.weights.stream().max(Double::compare);
        try{
            double weight = maxWeight.get();
            for(int i = 0; i < objects.size(); i++){
                if( weights.get(i).equals(weight)){
                    ret.add(objects.get(i));
                }
            }
        }
        catch(NoSuchElementException n){}
        return ret;
    }
    
    /**
     * Returns the first object with the lowest probability of being randomly chosen.
     * @return the first object with the lowest probability of being randomly chosen, null if it can't be found
     */
    public T getLeastProbable(){
        T ret;
        Optional<Double> minWeight = this.weights.stream().min(Double::compare);
        try{
            ret = this.objects.get(this.weights.indexOf(minWeight.get()));
        }
        catch(NoSuchElementException n){
            ret = null;
        }
        return ret;
    }
    
    /**
     * Returns the list of objects with the lowest probability of being randomly chosen.
     * @return the list of objects with the lowest probability of being randomly chosen,
     * null if they can't be found
     */
    public List<T> getAllLeastProbable(){
        List<T> ret = new ArrayList<>();
        Optional<Double> maxWeight = this.weights.stream().max(Double::compare);
        try{
            double weight = maxWeight.get();
            for(int i = 0; i < objects.size(); i++){
                if( weights.get(i).equals(weight)){
                    ret.add(objects.get(i));
                }
            }
        }
        catch(NoSuchElementException n){}
        return ret;
    }
    
    /**
     * Scale the weight of an object in the distribution by a factor.
     * New weight will be oldWeight*factor.
     * Probability bins are updated at the end of the operation.
     * @param o the object to update the weight of
     * @param factor the scale factor
     * @return true if the weight was scaled
     */
    public boolean scaleWeightOf(T o, double factor){
        boolean ret = false;
        int i = objects.indexOf(o);
        if(i>=0){
            weights.set(i, Math.abs(factor)*weights.get(i));
            ret = true;
            updateBins();
        }
        return ret;
    }
    
    /**
     * Scale all the weights of specified objects in the distribution by a factor.
     * New weights will be oldWeight*factor.
     * Probability bins are updated at the end of the operation.
     * @param o the list of objects to update the weight of
     * @param factor the scale factor
     * @return true if some of the weights were scaled
     */
    public boolean scaleAllWeightsOf(List<T> toScale, double factor){
        boolean ret = false;
        for(T o : toScale){
            int i = objects.indexOf(o);
            if(i>=0){
                weights.set(i, Math.abs(factor)*weights.get(i));
                ret = true;
            }
        }
        updateBins();
        return ret;
    }
    
    /**
     * Test method for this class
     * @param args unused parameter
     */
    public static void main(String[] args) {
        ObjectDistribution<String> od = new ObjectDistribution<>();
        
        System.out.println("Most probable element: "+od.getMostProbable());
        System.out.println("Least probable element: "+od.getLeastProbable());
        System.out.println("All the most probable elements: "+od.getAllMostProbable().toString());
        
        boolean add1 = od.add("a");
        boolean add2 = od.addAll(Arrays.asList("b","c","d","e","f","g"));
        double tw = od.getTotalWeight();
        System.out.println("Add1: "+add1+", Add 2: "+add2+"\n"+od.toString()+"\nTotal weight: "+tw);
        
        od.scaleAllWeightsOf(Arrays.asList("c","d","e"), 2.0);
        od.scaleWeightOf("g", 1.1);
        
        System.out.println("Some weights were scaled: "+od.toString());
        
        System.out.println("Most probable element: "+od.getMostProbable());
        System.out.println("Least probable element: "+od.getLeastProbable());
        System.out.println("All the most probable elements: "+od.getAllMostProbable().toString());
        
        System.out.println("\nSome random extractions:");
        for(int i = 0; i<10; i++){
            String extracted = od.getRandom();
            System.out.println("Extraction "+i+": "+extracted);
        }
        
        od.updateWeightSafely("z", 10.0);
        od.updateWeightSafely("a", 0.5);
        
        System.out.println("\nWeights updated.");
        tw = od.getTotalWeight();
        System.out.println(od.toString()+"\nTotal weight: "+tw);
        System.out.println("Most probable element: "+od.getMostProbable());
        System.out.println("Least probable element: "+od.getLeastProbable());
        System.out.println("All the most probable elements: "+od.getAllMostProbable().toString());
        
        System.out.println("\nSome more random extractions:");
        for(int i = 0; i<10; i++){
            String extracted = od.getRandom();
            System.out.println("Extraction "+i+": "+extracted);
        }
        
        od.updateWeightSafely("b", 10.0);
        od.updateWeightSafely("c", 0.5);
        
        System.out.println("\nWeights updated.");
        tw = od.getTotalWeight();
        System.out.println(od.toString()+"\nTotal weight: "+tw);
        System.out.println("Most probable element: "+od.getMostProbable());
        System.out.println("Least probable element: "+od.getLeastProbable());
        System.out.println("All the most probable elements: "+od.getAllMostProbable().toString());
        
        System.out.println("\nSome more random extractions:");
        for(int i = 0; i<10; i++){
            String extracted = od.getRandom();
            System.out.println("Extraction "+i+": "+extracted);
        }
        
    }
    
}

/**
 * This inner class represents a bin in a distribution of values
 * @author Frash
 */
class Bin{
    private double inf;
    private double sup;
    
    /**
     * Constructor for inner class Bin
     * @param inf
     * @param sup 
     */
    public Bin(double inf, double sup){
        this.inf = inf;
        this.sup = sup;
    }
    
    /**
     * Gets the lower bound of the bin
     * @return the lower bound of the bin
     */
    public double getInf() {
        return inf;
    }
    
    /**
     * Gets the upper bound of the bin
     * @return the upper bound of the bin
     */
    public double getSup() {
        return sup;
    }
    
    /**
     * Updates the lower and upper bounds for this bin, if the first is less than
     * the second.
     * @param inf new value of the lower bound
     * @param sup new value of the upper bound
     * @return <tt>true</tt> if the bin was updated correctly.
     */
    public boolean update(double inf, double sup){
        if(inf<sup){
            this.inf = inf;
            this.sup = sup;
            return true;
        }
        else return false;
    }
    
    /**
     * Returns true if the input value is in the interval between the
     * lower bound (inclusive) and the upper bound (exclusive) of this bin.
     * @param value
     * @return <tt>true</tt> if value is within the bin bounds
     */
    public boolean inBin(double value){
        return (value>=inf) && (value<sup);
    }
    
    @Override
    public String toString(){
        return "["+inf+", "+sup+"]";
    }
}