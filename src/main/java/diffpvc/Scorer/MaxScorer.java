package diffpvc.Scorer;

import diffpvc.*;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.io.Reader;
import java.io.FileReader;
import java.io.Serializable;
import java.math.BigDecimal;

import diffpvc.PrivacyAgents.PrivacyAgentBudget;

/**
 * User: Arik Friedman
 * Date: 10/09/2009
 * Time: 00:25:57
 */
public class MaxScorer extends AttributeScoreAlgorithm implements Serializable {

       // The Score is the total number of hits, assuming
       // that the majority class is the correct classification
       public double Score(Instances data, C45Attribute att) {
              Instances[] splitData = SplitData(data,att);
              double score=0;
              for (int j = 0; j < splitData.length; j++) {
                     if (splitData[j].numInstances() > 0) {
                            score+=numMajorityClass(splitData[j]);
                     }
              }
              return score;
       }

       public double GetSensitivity() {
              return 1;
       }

       public void InitializeMaxNumInstances(int num) {
              // NumMaxInstances is irrelevant to this scorer
       }

       /**
        * Score a binary split (used for evaluation splits on numeric attributes)
        *
        * @param leftDist  class counts in the first partition
        * @param rightDist class counts in the second partition
        * @return a score for the given split
        */
       @Override
       public double Score(int[] leftDist, int[] rightDist) {
              return leftDist[Utils.maxIndex(leftDist)] + rightDist[Utils.maxIndex(rightDist)];
       }

       private int numMajorityClass(Instances data)
       {
              double[] counts= new double[data.numClasses()];
              Enumeration instEnum = data.enumerateInstances();
              while (instEnum.hasMoreElements()) {
                     Instance inst = (Instance) instEnum.nextElement();
                     counts[(int) inst.classValue()]++;
              }
              int maxClassValue = Utils.maxIndex(counts);
              return (int) counts[maxClassValue];
       }    

       /**
        * The main method is used to test the MaxScorer
        * @param args program parameters, unused
        */
       @SuppressWarnings("unchecked")
       public static void main(String[] args)
       {
              //final String INPUT_FILE="contact-lenses.arff";
              final String INPUT_FILE="adult.merged.arff";
              Instances data;
              try {
                     Reader  reader = new FileReader(INPUT_FILE);
                     data = new Instances(reader);
              } catch (Exception e) {
                     System.out.println("Failed to read input file " + INPUT_FILE + ", exiting");
                     return;
              }
              data.setClassIndex(data.numAttributes()-1);

              System.out.println("Acquired data from file " + INPUT_FILE);
              System.out.println("Got " + data.numInstances() + " instances with " + data.numAttributes() + " attributes, class attribute is " + data.classAttribute().name());
              AttributeScoreAlgorithm scorer = new MaxScorer ();
              Enumeration<Attribute> atts=(Enumeration<Attribute>)data.enumerateAttributes();
              while (atts.hasMoreElements())
              {
                     C45Attribute att=new C45Attribute(atts.nextElement());
                     System.out.println("Score for attribute " + att.WekaAttribute().name() + " is " + scorer.Score(data,att));
              }

              List<C45Attribute> candidateAttributes = new LinkedList<C45Attribute>();
              Enumeration attEnum = data.enumerateAttributes();
              while (attEnum.hasMoreElements())
                     candidateAttributes.add(new C45Attribute((Attribute)attEnum.nextElement()));
              BigDecimal epsilon=new BigDecimal(0.1, DiffPrivacyClassifier.MATH_CONTEXT);
              PrivacyAgent agent = new PrivacyAgentBudget(epsilon);

              PrivateInstances privData = new PrivateInstances(agent,data);
              privData.setDebugMode(true);

              try {
                     C45Attribute res=privData.privateChooseAttribute(new MaxScorer(), candidateAttributes,epsilon);
                     System.out.println("Picked attribute " + res.WekaAttribute().name());
              } catch(Exception e) { System.out.println(e.getMessage());}
       }
}
