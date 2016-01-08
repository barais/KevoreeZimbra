package org.kevoree.zimbra;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.joda.time.format.ISODateTimeFormat;
import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Input;
import org.kevoree.annotation.KevoreeInject;
import org.kevoree.annotation.Output;
import org.kevoree.annotation.Param;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.cs.zclient.ZDateTime;
import com.zimbra.cs.zclient.ZMailbox;

@ComponentType(description = "this component return the next appoitement in json format {\"label\" : \"Cours\", \"date\" : \"2012-04-23T18:25:43.511Z\", \"duration\" : 3600,\"location\" : \"Amphi P\"    }")
public class ZimbraNextRDV {

	private ZMailbox client;

	@Param(defaultValue = "")
	String login;

	@Param(defaultValue = "")
	String pass;

	@Param(defaultValue = "10")
	String zimbraFolderId;


	@KevoreeInject
	org.kevoree.api.Context context;

	@Output
	org.kevoree.api.Port out;

	@Input
	public void in(Object i) {
		out.send(getLastSortedRdv());
	}

	private String getLastSortedRdv() {
		List<Appointement> lists = getSortedRdv();
		return lists.get(0).toString();
		
	}
	

	
	private List<Appointement> getSortedRdv() {
		try {
			client = ZMailbox.getByName(login, pass, "https://zimbra.inria.fr/service/soap/");
			XMLElement req = new XMLElement(ZimbraNamespace.E_BATCH_REQUEST);
			Element gas = req.addElement(MailConstants.GET_APPT_SUMMARIES_REQUEST);

			Calendar cal = Calendar.getInstance();
			Date today = cal.getTime();
	//		cal.add(Calendar.HOUR, -1); // to get previous year add -1
			gas.addAttribute(MailConstants.A_CAL_START_TIME, today.getTime());

			cal.add(Calendar.MONTH, 1); // to get previous year add -1
			Date nextYear = cal.getTime();

			gas.addAttribute(MailConstants.A_CAL_END_TIME, nextYear.getTime());
			gas.addAttribute(MailConstants.A_FOLDER, zimbraFolderId);
			Element resp = client.invoke(req);
			List<Appointement> lists = new ArrayList<Appointement>();

			for (Element e : resp.listElements()) {
				for (Element appt : e.listElements(MailConstants.E_APPOINTMENT)) {
					Appointement a = new Appointement();
					a.setLabel("" + appt.toXML().attribute("name").getData());
					a.setLocation("" + appt.toXML().attribute("loc").getData());
					String duration = "" + appt.toXML().attribute("d").getData();
					a.setDuration(Long.parseLong(duration) / 1000);
					ZDateTime dated = new ZDateTime("" + appt.toXML().element("inst").attribute("ridZ").getData());
					a.setDate(dated.getDate());
					lists.add(a);
				}
			}
			Collections.sort(lists, new Comparator<Appointement>() {
				
				public int compare(Appointement appt1, Appointement appt2) {
					return appt1.getDate().compareTo(appt2.getDate());
				}
			});

			return lists;
		} catch (ServiceException e) {
			e.printStackTrace();
		}
		return new ArrayList<Appointement>();
	}

	@Start
	public void start() {
	}

	@Stop
	public void stop() {
	}

}

class Appointement {

	String label;

	Date date;
	long duration = 0;
	String location;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return "{\"label\" : \"" + this.label + "\", \"date\" : \"" + ISODateTimeFormat.dateTimeNoMillis().print(this.date.getTime()) + "\", \"duration\" : " + this.duration
				+ ",\"location\" : \"" + this.location + "\"    }";
	}

}
