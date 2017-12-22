# alns-2017
## Version 1.0, December 22 2017
Solve the given instances for a Clustered Team Orienteering problem With Services Sequence (CTOWSS) using either an ALNS (Adaptive Large Neighborhood Search) algorithm, a regular MIPS solver or the MIPS solver on the model's relaxation.

(C) 2017 Francesco Piazza

## Where to download


## Minimum system requirements
### Software
This project has been coded for
- JDJ 1.8
- Gurobi 7.5.1
So you will need to provide those in order to run.

### Hardware
A multicore processor is highly advised in order to solve orienteering problems faster.

**An important note on hyperthreading:** Hyperthreading has been proven to be useless, so if you have an i7 processor which appears to have 8 logical cores in place of 4 physical cores, choose 4 threads only!

**TL;DR: Hyperthreading => threads = cores / 2**

## How to use
### GUI
1. Double click the JAR file
2. Load one instance using the panel on the left
3. Tweak the parameters (if needed)
4. Select an output folder
5. Select a solver from the panel on the right
6. Hit run and wait for the solver to finish. (You might want to take a coffee or two...)
7. You will find logs and outputs in the chosen folder

### CLI
1. Open a terminal or a command prompt located in your JAR folder
2. Run the software with the following command: `java -jar "CTOWSS_alns.jar" [options]`
For CLI options, follows these instructions:
```
usage: java -jar CTOWSS_alns.jar [-c <cores>] [-f <pathToInstance1>
       <pathToInstance2> <...>] [-h] [-o <output>] [-p <parametersPath>]
       [-s <solver>] [-t <time>]

 -c,--cores <cores>                                         number of
                                                            physical
                                                            cores/threads
                                                            for the solver
                                                            to use. If
                                                            your processor
                                                            has
                                                            hyperthreading
                                                            , use only
                                                            half of the
                                                            available
                                                            cores (eg: 8
                                                            virtual cores
                                                            -> 4 real
                                                            cores)!

 -f,--filenames <pathToInstance1> <pathToInstance2> <...>   list of space
                                                            separated
                                                            instance paths
                                                            (filenames)

 -h,--help                                                  show help

 -o,--output <output>                                       path to the
                                                            output folder
                                                            (default:
                                                            current
                                                            working
                                                            directory)

 -p,--parameters <parametersPath>                           path to the
                                                            JSON file
                                                            which stores
                                                            the run
                                                            parameters

 -s,--solver <solver>                                       solver to use.
                                                            Options:
                                                            SOLVE_ALNS
                                                            (default),
                                                            SOLVE_MIPS,
                                                            SOLVE_RELAXED
 
 -t,--time <time>                                           maximum time
                                                            for a solver
                                                            run (in
                                                            seconds)
Launch with no arguments to run the GUI.
```

#### Usage example
`java -jar "CTOWSS_alns.jar" --f Instance0.txt -o "out" -t 100 -c 4`

A few notes on this:
- Make sure the output folder path exists before you start a run.
- Instance0.txt is a test instance we included in the distribution binaries.


### Parameters
The default parameters have been tested on about 20 instances found in literature and are thought to work fine on these.
Parameters are self explanatory if you know the theory behind the algorithm, but will be more thoroughly explained in the future.

## Known issues
- Unfortunately batch processing doesn't work as of December 2017.
- The GUI interface is ugly, however everything should work.
If you find any unreported issues, feel free to report them.
