 UPDATE AD_Process 
 SET classname ='org.libero'||SUBSTRING(classname,15,70)
 WHERE SUBSTRING(classname,0,15) = 'org.eevolution';

 UPDATE AD_Form 
 SET classname ='org.libero'||SUBSTRING(classname,15,70)
 WHERE SUBSTRING(classname,0,15) = 'org.eevolution';
 
 UPDATE AD_Column  SET callout = null
 WHERE SUBSTRING(callout,0,21) = 'org.eevolution.model';
