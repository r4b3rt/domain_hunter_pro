package Tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;

import GUI.GUIMain;
import base.BackGroundButton;
import base.Commons;
import burp.BurpExtender;
import config.ConfigPanel;
import domain.CertInfo;
import title.WebIcon;
import utils.DomainNameUtils;
import utils.GrepUtils;
import utils.IPAddressUtils;

/**
 * 所有配置的修改，界面的操作，都立即写入LineConfig对象，如有必要保存到磁盘，再调用一次SaveConfig函数，思路要清晰
 * 加载： 磁盘文件-->LineConfig对象--->具体控件的值
 * 保存： 具体各个控件的值---->LineConfig对象---->磁盘文件
 */

public class ToolPanel extends JPanel {

	private JLabel projectLabel;

	PrintWriter stdout;
	PrintWriter stderr;
	public static JTextArea searchResultTextArea;
	public static JTextArea inputTextArea;
	public static JTextArea outputTextArea;

	public boolean inputTextAreaChanged = true;

	String history = "";
	JLabel statusLabel = new JLabel("");


	private GUIMain guiMain;

	public GUIMain getGuiMain() {
		return guiMain;
	}

	public void setGuiMain(GUIMain guiMain) {
		this.guiMain = guiMain;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					JFrame frame = new JFrame();
					frame.setVisible(true);
					frame.setContentPane(new ToolPanel(null));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ToolPanel(GUIMain guiMain) {
		this.guiMain = guiMain;
		setForeground(Color.DARK_GRAY);//构造函数
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.setLayout(new BorderLayout(0, 0));

		try {
			stdout = new PrintWriter(BurpExtender.getCallbacks().getStdout(), true);
			stderr = new PrintWriter(BurpExtender.getCallbacks().getStderr(), true);
		} catch (Exception e) {
			stdout = new PrintWriter(System.out, true);
			stderr = new PrintWriter(System.out, true);
		}


		///////////////////////HeaderPanel//////////////

		/*
		JPanel HeaderPanel = new JPanel();
		HeaderPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout fl_HeaderPanel = (FlowLayout) HeaderPanel.getLayout();
		fl_HeaderPanel.setAlignment(FlowLayout.LEFT);
		this.add(HeaderPanel, BorderLayout.NORTH);

		JLabel lblNewLabelNull = new JLabel("  ");
		HeaderPanel.add(lblNewLabelNull);
		 */



		///////////////////////BodyPane//////////////


		JPanel BodyPane = new JPanel();//中间的大模块，一分为二
		this.add(BodyPane, BorderLayout.CENTER);
		BodyPane.setLayout(new GridLayout(1, 2, 0, 0));

		//JScrollPanelWithHeaderForTool searhResultPanel = new JScrollPanelWithHeaderForTool("Search Result","",true);
		//searchResultTextArea = searhResultPanel.getTextArea();


		JScrollPanelWithHeaderForTool InputPanel = new JScrollPanelWithHeaderForTool("Input","",true,true);
		inputTextArea = InputPanel.getTextArea();
		inputTextArea.getDocument().addDocumentListener(new textAreaListener(inputTextArea));
		inputTextArea.addMouseListener(new TextAreaMouseListener(guiMain,inputTextArea));


		JScrollPanelWithHeaderForTool OutPanel = new JScrollPanelWithHeaderForTool("OutPut","",false,false);
		outputTextArea = OutPanel.getTextArea();
		outputTextArea.addMouseListener(new TextAreaMouseListener(guiMain,outputTextArea));

		JPanel buttonPanel = createButtons();

		BodyPane.add(InputPanel);
		BodyPane.add(OutPanel);

		this.add(buttonPanel, BorderLayout.EAST);//这样避免小屏幕按钮显示不完整！
		//BodyPane.add(buttonPanel);



		///////////////////////////FooterPanel//////////////////

		JPanel footerPanel = new JPanel();
		footerPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
		FlowLayout fl_FooterPanel = (FlowLayout) footerPanel.getLayout();
		fl_FooterPanel.setAlignment(FlowLayout.LEFT);
		this.add(footerPanel, BorderLayout.SOUTH);

		projectLabel = new JLabel(BurpExtender.getExtenderName() + "    " + BurpExtender.getGithub());
		projectLabel.setFont(new Font("宋体", Font.BOLD, 12));
		projectLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					URI uri = new URI(BurpExtender.getGithub());
					Desktop desktop = Desktop.getDesktop();
					if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
						desktop.browse(uri);
					}
				} catch (Exception e2) {
					e2.printStackTrace(stderr);
				}

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				projectLabel.setForeground(Color.BLUE);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				projectLabel.setForeground(Color.BLACK);
			}
		});
		footerPanel.add(projectLabel);

	}

	public JPanel createButtons() {

		JButton outputToInput = new BackGroundButton("Input<----Output") {

			@Override
			protected void action() {
				statusLabel.setText("doing");
				String content = outputTextArea.getText();
				if (null != content) {
					inputTextArea.setText(content);
				}
				statusLabel.setText("Done!!!");
			}

		};

		JButton btnFindDomains = new BackGroundButton("Find Domains") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				//stdout.println(content);
				if (null != content) {
					Set<String> domains = GrepUtils.grepDomain(content);
					ArrayList<String> tmpList = new ArrayList<String>(domains);
					Collections.sort(tmpList, new DomainComparator());
					outputTextArea.setText(String.join(System.lineSeparator(), tmpList));
					guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
				}
			}
		};

		JButton btnFindDomainsNoPort = new BackGroundButton("Find Domains(No Port)") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				//stdout.println(content);
				if (null != content) {
					Set<String> domains = GrepUtils.grepDomainNoPort(content);
					ArrayList<String> tmpList = new ArrayList<String>(domains);
					Collections.sort(tmpList, new DomainComparator());
					outputTextArea.setText(String.join(System.lineSeparator(), tmpList));
					guiMain.getDomainPanel().getDomainResult().addIfValid(domains);
				}
			}
		};


		JButton btnFindUrls = new BackGroundButton("Find URL") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = GrepUtils.grepURL(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		};


		JButton btnFindUrls1 = new BackGroundButton("Find URL 1") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> urls = GrepUtils.grepURL1(content);
					outputTextArea.setText(String.join(System.lineSeparator(), urls));
				}
			}
		};

		JButton btnCleanUrl = new BackGroundButton("Clean URL") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> lines = Commons.getLinesFromTextArea(inputTextArea);
					List<String> result = new ArrayList<String>();

					for (String item:lines) {
						if (GrepUtils.uselessExtension(item)) {
							continue;
						}else {
							result.add(item);
						}
					}//不在使用set方法去重，以便保持去重后的顺序！
					String output = String.join(System.lineSeparator(), result);
					outputTextArea.setText(output);
				}
			}
		};


		JButton btnFindIP = new BackGroundButton("Find IP") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = GrepUtils.grepIP(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};

		JButton btnFindPublicIP = new BackGroundButton("Find Public IP") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = GrepUtils.grepPublicIP(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};


		JButton btnFindPrivateIP = new BackGroundButton("Find Private IP") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = GrepUtils.grepPrivateIP(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};


		JButton btnFindIPAndPort = new BackGroundButton("Find IP:Port") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> iplist = GrepUtils.grepIPAndPort(content);
					outputTextArea.setText(String.join(System.lineSeparator(), iplist));
				}
			}
		};

		JButton btnFindPort = new BackGroundButton("Find Port") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> result = new ArrayList<String>();
					List<String> lines = Commons.textToLines(content);
					for (String line:lines) {
						List<String> portlist = GrepUtils.grepPort(line);
						result.addAll(portlist);
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}
		};


		JButton btnMasscanResultToNmap = new BackGroundButton("Masscan->Nmap"){

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content && !content.equals("")) {

					List<String> lines = Commons.textToLines(content);
					HashMap<String, Set<String>> ipAndPorts = new HashMap<String,Set<String>>();
					List<String> nmapCmds = new ArrayList<String>();
					for (String line:lines) {
						if (line.contains("Discovered open port")) {
							try {
								String port = line.split(" ")[3].split("/")[0];
								String host = line.split(" ")[5];
								Set<String>      ports = ipAndPorts.get(host);
								if (ports == null) {
									ports = new HashSet<String>();
								}
								ports.add(port);
								ipAndPorts.put(host, ports);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}

					for (String host:ipAndPorts.keySet()) {
						nmapCmds.add("nmap -v -A -p "+String.join(",", ipAndPorts.get(host))+" "+host);
					}

					outputTextArea.setText(String.join(System.lineSeparator(), nmapCmds));
				}
			}

		};

		JButton btnMasscanResultToHttp = new BackGroundButton("Masscan->Http"){

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content && !content.equals("")) {

					List<String> lines = Commons.textToLines(content);
					List<String> result = new ArrayList<String>();
					for (String line:lines) {
						if (line.contains("Discovered open port")) {
							try {
								String port = line.split(" ")[3].split("/")[0];
								String host = line.split(" ")[5];
								result.add("http://"+host+":"+port);
								result.add("https://"+host+":"+port);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}

		};


		JButton btnNmapResultToHttp = new BackGroundButton("Nmap->Http"){

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content && !content.equals("")) {
					List<String> result = new ArrayList<String>();

					List<String> iplist = GrepUtils.grepIP(content);
					List<String> lines = Commons.textToLines(content);

					for (String line:lines) {
						if (line.toLowerCase().contains("ssl")) {
							List<String> portlist = GrepUtils.grepPort(line);
							for (String port:portlist) {
								for (String host:iplist) {
									result.add("https://"+host+":"+port);
								}
							}
						}else if (line.toLowerCase().contains("http")) {
							List<String> portlist = GrepUtils.grepPort(line);
							for (String port:portlist) {
								for (String host:iplist) {
									result.add("http://"+host+":"+port);
								}
							}
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}

		};


		JButton btnNmapResultToHttp1 = new BackGroundButton("Nmap->Http 1"){

			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content && !content.equals("")) {

					List<String> result = new ArrayList<String>();

					List<String> iplist = GrepUtils.grepIP(content);
					List<String> portlist = GrepUtils.grepPort(content);

					for (String host:iplist) {
						for (String port:portlist) {
							result.add("http://"+host+":"+port);
							result.add("https://"+host+":"+port);
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				}
			}
		};



		JButton btnFindSubnet = new BackGroundButton("Find Subnet") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					List<String> subnets = GrepUtils.grepSubnet(content);
					outputTextArea.setText(String.join(System.lineSeparator(), subnets));
				}
			}
		};


		JButton btnFindEmail = new BackGroundButton("Find Email") {
			@Override
			protected void action() {
				String content = inputTextArea.getText();
				if (null != content) {
					Set<String> emails = GrepUtils.grepEmail(content);
					outputTextArea.setText(String.join(System.lineSeparator(), emails));
					guiMain.getDomainPanel().getDomainResult().addIfValidEmail(emails);
				}
			}
		};


		JButton btnOpenurls = new BackGroundButton("OpenURLs"){
			List<String> urls = new ArrayList<>();
			Iterator<String> it = urls.iterator();
			private int totalNumber;
			private int left;

			@Override
			protected void action() {
				if (inputTextAreaChanged) {//default is true
					urls = Commons.getLinesFromTextArea(inputTextArea);
					totalNumber = urls.size();
					left = urls.size();
					it = urls.iterator();
					inputTextAreaChanged = false;
				}
				try {
					int i = 50;
					while (i > 0 && it.hasNext()) {
						String url = it.next();
						if (!url.toLowerCase().startsWith("https://") &&
								!url.toLowerCase().startsWith("http://")) {
							url = "http://" + url;
							URL tmpUrl = new URL(url);
							if (tmpUrl.getPort() == -1) {
								Commons.browserOpen(url, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
								Commons.browserOpen(url.replaceFirst("http://", "https://"), guiMain.getConfigPanel().getLineConfig().getBrowserPath());
							} else if (Integer.toString(tmpUrl.getPort()).endsWith("443")) {
								Commons.browserOpen(url.replaceFirst("http://", "https://"), guiMain.getConfigPanel().getLineConfig().getBrowserPath());
							} else {
								Commons.browserOpen(url, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
							}
						} else {
							Commons.browserOpen(url, guiMain.getConfigPanel().getLineConfig().getBrowserPath());
						}
						i--;
						left--;
					}
					setText("OpenURLs" + "(" + left + "/" + totalNumber + ")");
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
				}
			}
		};



		JButton btnCertDomains = new BackGroundButton("GetCertDomains") {
			@Override
			protected void action() {
				ArrayList<String> result = new ArrayList<String>();
				List<String> urls = Commons.getLinesFromTextArea(inputTextArea);
				Iterator<String> it = urls.iterator();
				while (it.hasNext()) {
					String url = it.next();
					Set<String> domains = CertInfo.getAlternativeDomains(url);
					result.add(url + " " + domains.toString());
					System.out.println(url + " " + domains.toString());
				}
				outputTextArea.setText(String.join(System.lineSeparator(), result));
			}
		};


		JButton btnCertTime = new BackGroundButton("GetCertTime") {
			@Override
			protected void action() {
				ArrayList<String> result = new ArrayList<String>();
				List<String> urls = Commons.getLinesFromTextArea(inputTextArea);
				Iterator<String> it = urls.iterator();
				while (it.hasNext()) {
					String url = it.next();
					String time = CertInfo.getCertTime(url);
					result.add(url + " " + time);
					System.out.println(url + " " + time);
				}
				outputTextArea.setText(String.join(System.lineSeparator(), result));
			}
		};

		JButton btnCertIssuer = new BackGroundButton("GetCertIssuer") {
			@Override
			protected void action() {
				ArrayList<String> result = new ArrayList<String>();
				List<String> urls = Commons.getLinesFromTextArea(inputTextArea);
				Iterator<String> it = urls.iterator();
				while (it.hasNext()) {
					String url = it.next();
					String time = CertInfo.getCertIssuer(url);
					result.add(url + " " + time);
					System.out.println(url + " " + time);
				}
				outputTextArea.setText(String.join(System.lineSeparator(), result));
			}
		};

		JButton iconHashButton = new BackGroundButton("GetIconHash") {
			@Override
			protected void action() {
				try {
					ArrayList<String> result = new ArrayList<String>();
					List<String> urls = Commons.getLinesFromTextArea(inputTextArea);
					Iterator<String> it = urls.iterator();
					while (it.hasNext()) {
						String url = it.next();
						String hash = WebIcon.getHash(url,null);
						result.add(hash);
						System.out.println(url + " " + hash);
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};


		JButton getIPAddressButton = new BackGroundButton("GetIPAddress") {
			@Override
			protected void action() {
				try {
					ArrayList<String> result = new ArrayList<String>();
					List<String> domains = Commons.getLinesFromTextArea(inputTextArea);
					Iterator<String> it = domains.iterator();
					while (it.hasNext()) {
						String domain = it.next();
						if (IPAddressUtils.isValidIP(domain)) {//目标是一个IP
							result.add(domain);
						} else if (DomainNameUtils.isValidDomain(domain)) {//目标是域名
							HashMap<String, Set<String>> temp = DomainNameUtils.dnsquery(domain,null);
							Set<String> IPSet = temp.get("IP");
							result.addAll(IPSet);
						}
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};

		JButton rows2List = new BackGroundButton("Rows To List"){

			@Override
			protected void action() {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					outputTextArea.setText(content.toString());
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton rows2Array = new BackGroundButton("Rows To Array"){

			@Override
			protected void action() {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					for (int i = 0; i < content.size(); i++) {
						content.set(i, "\"" + content.get(i) + "\"");
					}

					outputTextArea.setText(String.join(",", content));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton removeDuplicate = new BackGroundButton("Deduplicate"){

			@Override
			protected void action() {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					List<String> result = new ArrayList<String>();

					for (String item:content) {
						if (result.contains(item)) {
							continue;
						}else {
							result.add(item);
						}
					}//不在使用set方法去重，以便保持去重后的顺序！
					String output = String.join(System.lineSeparator(), result);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton sort = new BackGroundButton("Sort"){

			@Override
			protected void action() {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist = new ArrayList<>(contentSet);

					Collections.sort(tmplist);
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton sortReverse = new BackGroundButton("Sort(Reverse Str)"){

			@Override
			protected void action() {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist = new ArrayList<>(contentSet);

					Collections.sort(tmplist,new ReverseStrComparator());
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton sortByLength = new BackGroundButton("Sort by Length"){

			@Override
			protected void action() {
				try {
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> contentSet = new HashSet<>(content);
					List<String> tmplist = new ArrayList<>(contentSet);

					Collections.sort(tmplist, new LengthComparator());
					String output = String.join(System.lineSeparator(), tmplist);
					outputTextArea.setText(output);
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}

		};


		JButton btnGrepJson = new BackGroundButton("Grep Json") {
			@Override
			protected void action() {
				try {
					String content = inputTextArea.getText();
					String toFind = JOptionPane.showInputDialog("to find which value", history);
					if (toFind == null) {
						return;
					} else {
						history = toFind;
						ArrayList<String> result = JSONHandler.grepValueFromJson(content, toFind);
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}

				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		};

		JButton btnGrepLine = new BackGroundButton("Grep Line") {
			@Override
			protected void action() {
				try {
					String toFind = JOptionPane.showInputDialog("to find which value", history);
					ArrayList<String> result = new ArrayList<String>();
					if (toFind == null) {
						return;
					} else {
						history = toFind;
						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							if (item.toLowerCase().contains(toFind.toLowerCase().trim())) {
								result.add(item);
							}
						}
						//outputTextArea.setText(result.toString());
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}

				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					//e1.printStackTrace(stderr);
				}
			}
		};


		/**
		 * grep line的反面。如果某行数据包含了指定的关键词，就从结果中移除
		 * 即 nagetive search
		 */
		JButton btnRemoveLine = new BackGroundButton("Remove Line") {
			@Override
			protected void action() {
				try {
					String toFind = JOptionPane.showInputDialog("remove lines which contain:", history);
					ArrayList<String> result = new ArrayList<String>();
					if (toFind == null) {
						return;
					} else {
						history = toFind;
						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							if (!item.toLowerCase().contains(toFind.toLowerCase().trim())) {
								result.add(item);
							}
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		};


		JButton btnRegexGrep = new BackGroundButton("Grep Regex") {
			@Override
			protected void action() {
				try {
					String toFind = JOptionPane.showInputDialog("Input Regex", history);

					if (toFind == null) {
						return;
					} else {
						history = toFind;
						ArrayList<String> result = new ArrayList<String>();
						String PATTERN = toFind;
						Pattern pRegex = Pattern.compile(PATTERN);
						String content = inputTextArea.getText();
						Matcher matcher = pRegex.matcher(content);
						while (matcher.find()) {//多次查找
							result.add(matcher.group());
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}
		};


		JButton btnAddPrefix = new BackGroundButton("Add Prefix/Suffix") {
			@Override
			protected void action() {
				try {
					String toAddPrefix = JOptionPane.showInputDialog("prefix to add", null);
					String toAddSuffix = JOptionPane.showInputDialog("suffix to add", null);
					ArrayList<String> result = new ArrayList<String>();
					if (toAddPrefix == null && toAddSuffix == null) {
						return;
					} else {
						if (toAddPrefix == null) {
							toAddPrefix = "";
						}

						if (toAddSuffix == null) {
							toAddSuffix = "";
						}

						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							item = toAddPrefix + item + toAddSuffix;
							result.add(item);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
				}
			}
		};


		JButton btnRemovePrefix = new BackGroundButton("Remove Prefix/Suffix") {
			@Override
			protected void action() {
				try {
					String Prefix = JOptionPane.showInputDialog("prefix to remove", null);
					String Suffix = JOptionPane.showInputDialog("suffix to remove", null);
					List<String> content = Commons.getLinesFromTextArea(inputTextArea);
					List<String> result = Commons.removePrefixAndSuffix(content, Prefix, Suffix);
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton btnReplace = new BackGroundButton("ReplaceFirstStr"){

			@Override
			protected void action() {
				try {
					String replace = JOptionPane.showInputDialog("string (from)", null);
					String to = JOptionPane.showInputDialog("replacement (to)", null);
					ArrayList<String> result = new ArrayList<String>();
					if (replace == null && to == null) {
						return;
					} else {
						if (replace == null) {
							replace = "";
						}

						if (to == null) {
							to = "";
						}

						replace = Pattern.quote(replace);
						List<String> content = Commons.getLinesFromTextArea(inputTextArea);
						for (String item : content) {
							item = item.replaceFirst(replace, to);
							result.add(item);
						}
						outputTextArea.setText(String.join(System.lineSeparator(), result));
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton btnIPsToCIDR = new BackGroundButton("IPs To CIDR"){

			@Override
			protected void action() {
				try {
					List<String> IPs = Commons.getLinesFromTextArea(inputTextArea);
					Set<String> subnets = IPAddressUtils.toSmallerSubNets(new HashSet<String>(IPs));

					List<String> tmplist = new ArrayList<>(subnets);//排序
					Collections.sort(tmplist);

					outputTextArea.setText(String.join(System.lineSeparator(), tmplist));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};

		JButton btnCIDRToIPs = new BackGroundButton("CIDR To IPs"){

			@Override
			protected void action() {
				try {
					List<String> subnets = Commons.getLinesFromTextArea(inputTextArea);
					List<String> IPs = IPAddressUtils.toIPList(subnets);// 当前所有title结果计算出的IP集合
					outputTextArea.setText(String.join(System.lineSeparator(), IPs));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton unescapeJava = new BackGroundButton("UnescapeJava"){

			@Override
			protected void action() {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeJava(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton unescapeHTML = new BackGroundButton("UnescapeHTML"){

			@Override
			protected void action() {
				try {
					outputTextArea.setText(StringEscapeUtils.unescapeHtml4(inputTextArea.getText()));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		JButton Base64ToFile = new BackGroundButton("Base64ToFile"){

			@Override
			protected void action() {
				try {
					byte[] payloadBytes = Base64.getDecoder().decode(inputTextArea.getText());
					File downloadFile = saveDialog();
					if (downloadFile != null) {
						FileUtils.writeByteArrayToFile(downloadFile, payloadBytes);
					}
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

			public File saveDialog() {
				try {
					JFileChooser fc = new JFileChooser();
					if (fc.getCurrentDirectory() != null) {
						fc = new JFileChooser(fc.getCurrentDirectory());
					} else {
						fc = new JFileChooser();
					}

					fc.setDialogType(JFileChooser.CUSTOM_DIALOG);

					int action = fc.showSaveDialog(null);

					if (action == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						return file;
					}
					return null;
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

		};


		JButton splitButton = new BackGroundButton("Split"){

			@Override
			protected void action() {
				String separator = JOptionPane.showInputDialog("input separator", null);
				if (separator != null) {//&& !separator.trim().equals("")有可能本来就是空格分割，所以不能有这个条件
					String text = inputTextArea.getText();
					String[] items = text.split(separator);
					outputTextArea.setText(String.join(System.lineSeparator(), items));
				}
			}

		};


		JButton combineButton = new BackGroundButton("Combine"){

			@Override
			protected void action() {
				String separator = JOptionPane.showInputDialog("input connect char", null);
				if (separator != null) {// && !separator.trim().equals("")
					List<String> items = Commons.getLinesFromTextArea(inputTextArea);
					outputTextArea.setText(String.join(separator, items));
				}
			}

		};


		JButton toLowerCaseButton = new BackGroundButton("toLowerCase"){

			@Override
			protected void action() {
				outputTextArea.setText(inputTextArea.getText().toLowerCase());
			}

		};


		JButton OpenFileButton = new BackGroundButton("Open File"){

			@Override
			protected void action() {
				String dir = ((SuperJTextArea) inputTextArea).getTextAsDisplay();
				try {
					Desktop.getDesktop().open(new File(dir));
				} catch (IOException e1) {
					e1.printStackTrace(stderr);
					statusLabel.setText("your input is not a valid path or file");
				}
			}

		};


		JButton testButton = new BackGroundButton("test"){

			@Override
			protected void action() {
				try {
					outputTextArea.setText(WebIcon.getHash(inputTextArea.getText(),null));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};

		JButton trimButton = new BackGroundButton("Trim/Strip"){

			@Override
			protected void action() {
				try {
					ArrayList<String> result = new ArrayList<String>();
					List<String> items = Commons.getLinesFromTextArea(inputTextArea);
					for (String item:items) {
						item = item.replace("\u00A0", " ").replace("\u2002", " ").replace("\u2003", " ").
								replace("\u2004", " ").replace("\u2005", " ").replace("\u2006", " ").
								replace("\u2007", " ").replace("\u2008", " ").replace("\u2009", " ").
								replace("\u200A", " ").replace("\u3000", " ");
						try {
							item = item.strip();
						} catch (Exception e) {
						}
						try {
							item = item.trim();
						} catch (Exception e) {
						}
						result.add(item);
					}
					outputTextArea.setText(String.join(System.lineSeparator(), result));
				} catch (Exception e1) {
					outputTextArea.setText(e1.getMessage());
					e1.printStackTrace(stderr);
				}
			}

		};


		/*
		JButton JsonSimplify = new BackGroundButton("Simplify Json");

		JsonSimplify.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = ((SuperJTextArea) inputTextArea).getTextAsDisplay();
				try {
					Gson gson = new GsonBuilder().create();
					String json = gson.toJson(text);
					outputTextArea.setText(json);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
					statusLabel.setText("your input is not a valid json");
				}
			}
		});


		JButton JsonBeautify = new BackGroundButton("Beautify Json");

		JsonBeautify.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = ((SuperJTextArea) inputTextArea).getTextAsDisplay();
				try {
					Gson gson = new GsonBuilder().setPrettyPrinting().create();
					String json = gson.toJson(text);
					outputTextArea.setText(json);
				} catch (Exception e1) {
					e1.printStackTrace(stderr);
					statusLabel.setText("your input is not a valid json");
				}
			}
		});
		 */

		//buttonPanel，里面放操作按钮
		JPanel buttonPanel = new JPanel();
		//buttonPanel.setLayout(new GridLayout(10, 1));
		//https://stackoverflow.com/questions/5709690/how-do-i-make-this-flowlayout-wrap-within-its-jsplitpane
		//buttonPanel.setMinimumSize(new Dimension(0, 0));//为了让button自动换行

		GridBagLayout gbl_buttonPanel = new GridBagLayout();
		buttonPanel.setLayout(gbl_buttonPanel);

		//查找提取类
		int rowIndex = 0;
		int cloumnIndex = 0;

		buttonPanel.add(outputToInput, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(statusLabel, new bagLayout(rowIndex, ++cloumnIndex));
		//buttonPanel.add(SearchToInput, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindDomains, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindDomainsNoPort, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindUrls, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindUrls1, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCleanUrl, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindIP, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindPublicIP, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindPrivateIP, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindSubnet, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnFindIPAndPort, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindPort, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnFindEmail, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnMasscanResultToNmap,new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnMasscanResultToHttp,new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnNmapResultToHttp,new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnNmapResultToHttp1,new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnGrepJson, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnGrepLine, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnRegexGrep, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnRemoveLine, new bagLayout(rowIndex, ++cloumnIndex));

		//网络请求类
		cloumnIndex = 0;
		buttonPanel.add(btnOpenurls, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(getIPAddressButton, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnCertDomains, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCertTime, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCertIssuer, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(iconHashButton, new bagLayout(rowIndex, ++cloumnIndex));

		//数据转换类
		cloumnIndex = 0;
		buttonPanel.add(rows2List, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(rows2Array, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnIPsToCIDR, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(btnCIDRToIPs, new bagLayout(rowIndex, ++cloumnIndex));


		cloumnIndex = 0;
		buttonPanel.add(removeDuplicate, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnReplace, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(trimButton, new bagLayout(rowIndex, ++cloumnIndex));


		cloumnIndex = 0;
		buttonPanel.add(sort,new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(sortReverse,new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(sortByLength, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(btnAddPrefix, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(btnRemovePrefix, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(unescapeJava, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(unescapeHTML, new bagLayout(rowIndex, ++cloumnIndex));
		//buttonPanel.add(JsonSimplify, new bagLayout(rowIndex, ++cloumnIndex) );
		//buttonPanel.add(JsonBeautify, new bagLayout(rowIndex, ++cloumnIndex) );

		cloumnIndex = 0;
		buttonPanel.add(toLowerCaseButton, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(splitButton, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(combineButton, new bagLayout(rowIndex, ++cloumnIndex));
		buttonPanel.add(Base64ToFile, new bagLayout(rowIndex, ++cloumnIndex));

		cloumnIndex = 0;
		buttonPanel.add(OpenFileButton, new bagLayout(++rowIndex, ++cloumnIndex));
		buttonPanel.add(testButton, new bagLayout(rowIndex, ++cloumnIndex));


		return buttonPanel;
	}

	public static Set<String> getSetFromTextArea(JTextArea textarea) {
		//user input maybe use "\n" in windows, so the System.lineSeparator() not always works fine!
		Set<String> domainList = new HashSet<>(Arrays.asList(textarea.getText().replaceAll(" ", "").replaceAll("\r\n", "\n").split("\n")));
		domainList.remove("");
		return domainList;
	}

	public static String getContentFromFile(String filename) {
		File tmpfile = new File(filename);
		if (tmpfile.exists() && tmpfile.isFile()) {
			try {
				return FileUtils.readFileToString(tmpfile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	//保存文本框的数据
	class textAreaListener implements DocumentListener {

		private JTextArea textArea;

		textAreaListener(JTextArea inputTextArea){
			this.textArea = inputTextArea;
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			if (ConfigPanel.listenerIsOn) {
				guiMain.getConfigPanel().getLineConfig().setToolPanelText(((SuperJTextArea) textArea).getTextAsDisplay());
				inputTextAreaChanged = true;
			}
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			if (ConfigPanel.listenerIsOn) {
				guiMain.getConfigPanel().getLineConfig().setToolPanelText(((SuperJTextArea) textArea).getTextAsDisplay());
				inputTextAreaChanged = true;
			}
		}

		@Override
		public void changedUpdate(DocumentEvent arg0) {
			if (ConfigPanel.listenerIsOn) {
				guiMain.getConfigPanel().getLineConfig().setToolPanelText(((SuperJTextArea) textArea).getTextAsDisplay());
				inputTextAreaChanged = true;
			}
		}
	}


	class bagLayout extends GridBagConstraints {
		/**
		 * 采用普通的行列计数，从1开始
		 *
		 * @param row
		 * @param column
		 */
		bagLayout(int row, int column) {
			this.fill = GridBagConstraints.BOTH;
			this.insets = new Insets(0, 0, 5, 5);
			this.gridx = column - 1;
			this.gridy = row - 1;
		}
	}

}
