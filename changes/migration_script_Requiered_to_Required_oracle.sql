SET SQLBLANKLINES ON
SET DEFINE OFF

-- Sep 22, 2013 6:19:57 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Element SET ColumnName='DurationRequired', Name='Duration Required', PrintName='Duration Required',Updated=TO_DATE('2013-09-22 18:19:57','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Element_ID=53284
;

-- Sep 22, 2013 6:19:57 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Column SET ColumnName='DurationRequired', Name='Duration Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53284
;

-- Sep 22, 2013 6:19:57 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Process_Para SET ColumnName='DurationRequired', Name='Duration Required', Description=NULL, Help=NULL, AD_Element_ID=53284 WHERE UPPER(ColumnName)='DURATIONREQUIRED' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Sep 22, 2013 6:19:57 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Process_Para SET ColumnName='DurationRequired', Name='Duration Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53284 AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:19:57 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_InfoColumn SET ColumnName='DurationRequired', Name='Duration Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53284 AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:19:57 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Field SET Name='Duration Required', Description=NULL, Help=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=53284) AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:19:58 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_PrintFormatItem SET PrintName='Duration Required', Name='Duration Required' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=53284)
;

-- Sep 22, 2013 6:20:49 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Element SET ColumnName='QtyRequired', Name='Qty Required',Updated=TO_DATE('2013-09-22 18:20:49','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Element_ID=53288
;

-- Sep 22, 2013 6:20:49 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Column SET ColumnName='QtyRequired', Name='Qty Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53288
;

-- Sep 22, 2013 6:20:49 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Process_Para SET ColumnName='QtyRequired', Name='Qty Required', Description=NULL, Help=NULL, AD_Element_ID=53288 WHERE UPPER(ColumnName)='QTYREQUIRED' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Sep 22, 2013 6:20:49 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Process_Para SET ColumnName='QtyRequired', Name='Qty Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53288 AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:20:49 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_InfoColumn SET ColumnName='QtyRequired', Name='Qty Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53288 AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:20:49 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Field SET Name='Qty Required', Description=NULL, Help=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=53288) AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:20:49 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_PrintFormatItem SET PrintName='Qty Required', Name='Qty Required' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=53288)
;

-- Sep 22, 2013 6:22:43 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Element SET ColumnName='SetupTimeRequired', Name='Setup Time Required', PrintName='Setup Time Required',Updated=TO_DATE('2013-09-22 18:22:43','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Element_ID=53291
;

-- Sep 22, 2013 6:22:43 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Column SET ColumnName='SetupTimeRequired', Name='Setup Time Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53291
;

-- Sep 22, 2013 6:22:43 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Process_Para SET ColumnName='SetupTimeRequired', Name='Setup Time Required', Description=NULL, Help=NULL, AD_Element_ID=53291 WHERE UPPER(ColumnName)='SETUPTIMEREQUIRED' AND IsCentrallyMaintained='Y' AND AD_Element_ID IS NULL
;

-- Sep 22, 2013 6:22:43 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Process_Para SET ColumnName='SetupTimeRequired', Name='Setup Time Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53291 AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:22:43 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_InfoColumn SET ColumnName='SetupTimeRequired', Name='Setup Time Required', Description=NULL, Help=NULL WHERE AD_Element_ID=53291 AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:22:43 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_Field SET Name='Setup Time Required', Description=NULL, Help=NULL WHERE AD_Column_ID IN (SELECT AD_Column_ID FROM AD_Column WHERE AD_Element_ID=53291) AND IsCentrallyMaintained='Y'
;

-- Sep 22, 2013 6:22:43 PM MYT
-- IDEMPIERE-1298 2Pack: Support copying of data from one client to another
UPDATE AD_PrintFormatItem SET PrintName='Setup Time Required', Name='Setup Time Required' WHERE IsCentrallyMaintained='Y' AND EXISTS (SELECT * FROM AD_Column c WHERE c.AD_Column_ID=AD_PrintFormatItem.AD_Column_ID AND c.AD_Element_ID=53291)
;


