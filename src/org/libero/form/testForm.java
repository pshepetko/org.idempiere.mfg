
package org.libero.form;

import java.io.IOException;
import java.text.SimpleDateFormat;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;
import org.adempiere.webui.session.SessionManager;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.util.Env;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Iframe;


public class testForm  implements IFormController,EventListener {

 	private CustomForm form = new CustomForm();
	private Iframe iframe = new Iframe();

//	private Label lversion = new Label(" [v1.00]");

 	private int height_map =0;

	private SimpleDateFormat date_time = new SimpleDateFormat("HHmmss");
	private String Locale = Env.getContext(Env.getCtx(), "#Locale");
	 		
	public testForm()
	{
		try
		{
			dynInit();
			zkInit();
			//dynInit();
			Borderlayout contentLayout = new Borderlayout();
			ZKUpdateUtil.setWidth(contentLayout, "100%");
			ZKUpdateUtil.setHeight(contentLayout, "100%");
 								
//			form.appendChild(lversion);			
			form.appendChild(iframe); 
//		 	iframe.setSrc("http://admin:admin@localhost:8082");		
//		 	iframe.setSrc("http://localhost:8082");		
			iframe.setSrc("http://idempiere.org");	
			iframe.invalidate();
		}
		catch(Exception e)
		{
			//log.log(Level.SEVERE, "", e);
		}
	}
	

	private void zkInit() throws Exception
	{
		int height = Double.valueOf(SessionManager.getAppDesktop().getClientInfo().desktopHeight * 0.8).intValue();
 
		height = height - 5;
		height_map=height;
		iframe.setHeight(height + "px");
		iframe.setWidth("100%");
		iframe.setAutohide(true);
		form.setWidth("100%");
		form.setHeight("100%");
		form.appendChild(iframe);

	}
	
	/**
	 * 	Dynamic Init.
	 */
	private void dynInit()
	{
		
	}	//	dynInit
	
	/**
	 * 	Dispose
	 */
	public void dispose()
	{
		SessionManager.getAppDesktop().closeActiveWindow();
	}	//	dispose
	
	public ADForm getForm() {
		return form;
	}
	
	/**************************************************************************
	 *  Action Listener
	 *  @param e event
	 * @throws IOException 
	 */
	public void onEvent(Event e) throws IOException
	{
	}   //  onEvent
	
}
