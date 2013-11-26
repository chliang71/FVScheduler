package scheduler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

public class InformationCollector {
	
	Selector selector;
	ServerSocketChannel ssc;
	AtomicInteger channelId;
	Gson gson;
	HashMap<Integer, int[][]> matrixMap;
	HashMap<Integer, String> updateInfo;
	int[] switchNum;
	int currentNumofSwitch;
	int currentNumofController;
	Thread collector;
	boolean stopCollecting;
	
	public InformationCollector() {
		gson = new Gson();
		channelId = new AtomicInteger(0);
		matrixMap = new HashMap<Integer, int[][]>();
		updateInfo = new HashMap<Integer, String>();
		stopCollecting = false;
	}
	
	public void startCollecting() {
		collector = new Thread(new CollectorThread());
		collector.start();
	}
	
	public void stopCollecting() {
		stopCollecting = true;
	}
	
	public int[][] retriveMatrix() {
		//return the merge matrix from all channels
		//first of all, we know that all matrixes have the 
		//same number of lines(each fv is connected to all controllers)
		//but the number of columns needs to be added
		int ncolumns = 0;
		int nrows = 0;
		int index = 0;
		for(int[][] m : matrixMap.values()) {
			ncolumns += m[0].length;
			nrows = m.length;
			switchNum[index] = ncolumns;
			index ++;
		}
		int[][] total = new int[nrows][ncolumns];
		int offset = 0;
		for(int i = 0;i<nrows;i++)
			for(int j = 0;j<ncolumns;j++)
				total[i][j] = 0;
		for(int[][] m : matrixMap.values()) {
			for(int i = 0;i<m.length;i++)
				for(int j = 0;j<m[0].length;j++)
					total[i][j+offset] = m[i][j];
			offset += m[0].length;
		}
		currentNumofSwitch = ncolumns;
		currentNumofController = nrows;
		return total;
	}
	
	public int getNumSwitch() {
		return currentNumofSwitch;
	}
	
	public int getNumControllre() {
		return currentNumofController;
	}
	
	public void buildUpdate(int[] assignment) {
		if(assignment.length != currentNumofSwitch) {
			System.out.println("PANIC! Inconsistent mapping");
			return;
		}
		int remaining = assignment.length;
		int index = 0;
		while(remaining != 0) {
			int[] cur = new int[switchNum[index]];
			for(int i=0;i<cur.length;i++) {
				cur[i] = assignment[assignment.length - remaining];
				remaining--;
			}
			String s = gson.toJson(cur);
			updateInfo.put(index, s);
		}
	}
	
	class CollectorThread implements Runnable {
		@Override
		public void run() {

			try {
				selector = Selector.open();
				ssc = ServerSocketChannel.open(); 
				ssc.configureBlocking(false);  
				ssc.socket().setReuseAddress(true);
				ssc.socket().bind(new InetSocketAddress(12345)); 
				ssc.register(selector, SelectionKey.OP_ACCEPT, "Server"); 

				while(!stopCollecting) {
					selector.select();  
					for(SelectionKey key : selector.selectedKeys()) {
						if (!key.isValid())
							continue;
						if((key.readyOps() & SelectionKey.OP_ACCEPT)
							     == SelectionKey.OP_ACCEPT) {
							ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
							SocketChannel sc = ssc.accept();
							sc.configureBlocking( false );
							sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new AtomicInteger(channelId.addAndGet(1)));
							
							synchronized(switchNum) {
								if(switchNum == null) 
									switchNum = new int[1];
								else 
									switchNum = new int[switchNum.length + 1];
							}
						} else if ((key.readyOps() & SelectionKey.OP_READ)
							     == SelectionKey.OP_READ) {
						     // Read the data
						     SocketChannel sc = (SocketChannel)key.channel();
						     ByteBuffer buffer = ByteBuffer.allocate(1024);
						     sc.read(buffer);
						     buffer.flip();
						     String s = new String(buffer.array());
						     int id = ((AtomicInteger)key.attachment()).get();
						     System.out.println("collector:" + s + " from " + id);
						     buffer.clear();
						     int[][] matrix = gson.fromJson(s, int[][].class);
						     matrixMap.put(id, matrix);
						     
						} else if ((key.readyOps() & SelectionKey.OP_WRITE)
							     == SelectionKey.OP_WRITE) {
							//SocketChannel sc = (SocketChannel)key.channel();
							//ByteBuffer buffer = ByteBuffer.wrap("Looks good".getBytes());
						    //sc.write(buffer);	
							int id = ((AtomicInteger)key.attachment()).get();
							if(updateInfo.containsKey(id)) {
								SocketChannel sc = (SocketChannel)key.channel();
								ByteBuffer buffer = ByteBuffer.wrap(updateInfo.get(id).getBytes());
							    sc.write(buffer);	
							    updateInfo.remove(id);
							}
						}
					} 
					selector.selectedKeys().clear();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
