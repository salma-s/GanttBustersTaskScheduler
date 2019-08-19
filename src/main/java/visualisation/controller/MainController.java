package visualisation.controller;

import algorithm.Algorithm;
import algorithm.AlgorithmBuilder;
import app.App;
import com.jfoenix.controls.JFXTreeTableView;
import fileio.IIO;
import graph.Graph;
import graph.GraphNode;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingNode;
import javafx.scene.chart.*;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.*;
import java.util.List;

public class MainController implements IObserver, Initializable {

    //Private Fields
    private IObservable _observableAlgorithm;
    private Graph _algorithmGraph;
    private SingleGraph _graphStream;
    private IIO _io;
    private GraphManager _graphManager;
    private GraphUpdater _graphUpdater;
    private Map<String, GraphNode> _algorithmResultMap;
    private AnimationTimer _animationTimer;


    //Public Control Fields from the FXML
    public HBox mainContainer;
    public VBox statsContainer;
    public Text algorithmStatus;
    public Text algorithmTypeText;
    public Text timeElapsedText;
    public Text numberOfTasks;
    public Text numberOfProcessors;
    public Text numberOfThreads;
    public Text currentBestSchedule;
    public Text branchesBounded;
    public Text branchesPruned;
    public Text statesGenerated;
    public TabPane visualsContainer;

    public Tab graphTab;
    public Pane graphPane;
    public SwingNode swingNode;

    public Tab taskTab;
    public Pane ganttPane;

    public Tab resultTab;
    public JFXTreeTableView<?> scheduleResultsTable;
    public TreeTableColumn<?, ?> taskIDColumn;
    public TreeTableColumn<?, ?> startTimeColumn;
    public TreeTableColumn<?, ?> assignedProcessorColumn;

    public MainController(){

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        _io = App._mainIO;

        //GUI
        _graphManager = new GraphManager(_io.getNodeMap(),_io.getEdgeList());
        initializeGraph();
        initializeGantt();
        initializeStatistics();

        //TODO: None of the code below this can be in the initialize method because this occurs before the screen renders.
        // This means the algorithm/timer starts and sometimes stops before user can even see this. Please yeet this
        // somehow to make this not an issue
        //Algorithm
        Graph graph = new Graph(_io.getNodeMap(), _io.getEdgeList()); //create graph from nodes and edges
        Algorithm algorithm = AlgorithmBuilder.getAlgorithmBuilder().createAlgorithm(graph, _io.getNumberOfProcessorsForTask(), _io.getNumberOfProcessorsForParallelAlgorithm()).getAlgorithm();  //call algorithm graph
        _observableAlgorithm = AlgorithmBuilder.getAlgorithmBuilder().getAlgorithm();
        algorithmTypeText.setText(algorithmTypeText.getText() + AlgorithmBuilder.getAlgorithmBuilder().getAlgorithmType());
        _algorithmGraph = _observableAlgorithm.getAlgorithmGraph();
        algorithm.add(this);
        startTimer();
        //Runs the algorithm in a new thread
        new Thread() {
            public void run() {
                _io.write(algorithm.solveAlgorithm());
            }
        }.start();
    }

    @Override
    public void updateGraph() {
        _observableAlgorithm.getCurrentBestSolution();
        //Run on another thread
        Platform.runLater(() -> {
            //update graph visualization using runnable
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    for (Node node : _graphStream) {
                        //TODO: Receive and update node states via GraphUpdater
                    }
                }
            });
        });
    }

    @Override
    public void stopTimer() {
        algorithmStatus.setText("Status: Done");
        _animationTimer.stop();
        _algorithmResultMap = _observableAlgorithm.getCurrentBestSolution();
    }

    private void initializeGraph() {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer"); //Set styling renderer
        _graphStream = _graphManager.getGraph();
        _graphUpdater = new GraphUpdater(_graphStream, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        _graphUpdater.enableAutoLayout();

        //Create graphstream view panel
        ViewPanel viewPanel = _graphUpdater.addDefaultView(false);
        viewPanel.setMinimumSize(new Dimension(700,500)); //Window size
        viewPanel.setOpaque(false);
        viewPanel.setBackground(Color.white);
        _graphUpdater.setProcessorColours(_io.getNumberOfProcessorsForTask());
        //_graphUpdater.setMouseManager(viewPanel); //Disable mouse drag of nodes

        //Assign graph using swing node
        SwingUtilities.invokeLater(() -> {
            swingNode.setContent(viewPanel);
        });
        swingNode.setLayoutX(5);
        swingNode.setLayoutY(5);
    }

    private void initializeGantt() {
        _algorithmResultMap = _io.getAlgorithmResultMap();

        //Gantt chart initialize
        final NumberAxis xAxis = new NumberAxis();
        final CategoryAxis yAxis = new CategoryAxis();
        final GanttChart<Number, String> ganttChart = new GanttChart<>(xAxis, yAxis);
        ganttPane.getChildren().add(ganttChart);
        ganttChart.getStylesheets().add(getClass().getResource("/view/stylesheet.css").toExternalForm()); //style

        //ganttchart fx properties
        ganttChart.setPrefWidth(640);
        ganttChart.setLayoutX(20);
        ganttChart.setLayoutY(40);
        ganttChart.setLegendVisible(false);
        ganttChart.setBlockHeight(60);

        //y axis (processor count)
        List<String> machines = new ArrayList<>();
        for (int i = 0; i < _io.getNumberOfProcessorsForTask(); i++) {
            machines.add("Processor " + Integer.toString(i));
        }
        yAxis.setLabel("");
        yAxis.setTickLabelGap(10);
        yAxis.setCategories(FXCollections.observableList(machines));

        //x axis (xValue=Starttime, lengthMs=Worktime)
        xAxis.setLabel("Start time (s)");
        xAxis.setMinorTickCount(10);
        for (String processor : machines) {
            XYChart.Series series1 = new XYChart.Series();
            series1.getData().add(new XYChart.Data(0, processor, new GanttChart.ExtraData(1, "status-red")));
            series1.getData().add(new XYChart.Data(1, processor, new GanttChart.ExtraData(2, "status-red")));
            series1.getData().add(new XYChart.Data(3, processor, new GanttChart.ExtraData(3, "status-red")));
            series1.getData().add(new XYChart.Data(6, processor, new GanttChart.ExtraData(4, "status-red")));
            ganttChart.getData().addAll(series1);
        }
    }

    private void initializeStatistics() {
        algorithmStatus.setText(algorithmStatus.getText() + "In progress");
        numberOfTasks.setText(numberOfTasks.getText() + _io.getNodeMap().size());
        numberOfProcessors.setText(numberOfProcessors.getText() + _io.getNumberOfProcessorsForTask());
        numberOfThreads.setText(numberOfThreads.getText() + _io.getNumberOfProcessorsForParallelAlgorithm());
    }

    //TODO: Refactor this stuff out
    public void setTimerStatistic(long currentTime) {
        Platform.runLater(() -> {

            long minutes = (currentTime / 60000);
            long seconds = ((currentTime - minutes * 60) / 1000);
            long milliseconds = (currentTime - minutes * 60 - seconds * 1000) / 10;

            String minutesText ="";
            String secondsText = "";
            String millisecondsText = "";
            if (seconds % 60 < 10) { //Fix seconds
                secondsText = "0" + seconds % 60;
            } else {
                secondsText = Long.toString(seconds % 60);
            }

            if (minutes < 10) {//Fix minutes
                minutesText = "0" + minutes;
            } else {
                minutesText = Long.toString(minutes);
            }

            if (milliseconds < 10) {
                millisecondsText = "00" + millisecondsText;
            } else {
                millisecondsText = Long.toString(milliseconds);
            }

            timeElapsedText.setText("Time Elapsed: " + minutesText + " : " + secondsText + " : " + millisecondsText);
        });
    }

    //TODO: Refactor this crap outta here
    public void startTimer() {
        _animationTimer = new AnimationTimer() {
            private long timestamp;
            private long time = 0;
            @Override
            public void start() {
                // current time adjusted by remaining time from last run
                timestamp = System.currentTimeMillis();
                super.start();
            }
            @Override
            public void handle(long now) {
                long newTime = System.currentTimeMillis();
                if (timestamp <= newTime) {
                    time += (newTime - timestamp);
                    timestamp += (newTime - timestamp);
                    setTimerStatistic(time);
                }
            }
        };
        _animationTimer.start(); //start timer
    }
}
