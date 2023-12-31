/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hrdcorp.ncs_dev;


import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SetupManager;
import org.joget.commons.util.UuidGenerator;

/**
 *
 * @author Fawad Khaliq <khaliq@opendynamics.com.my>
 */

// This is used in form (store binder) to store audit trail after submitting form. ex: https://ncs-dev.hrdcorp.gov.my/jw/web/console/app/course_registration_module/1/form/builder/cr_review > Settings > Save data to

public class CourseAuditTrailBinder extends WorkflowFormBinder {

    public CourseAuditTrailBinder getSuper() {
        return this;
    }

    Connection con = null;
    
    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {

        // store data contained in joget form using default joget store function
        super.store(element, rows, formData);
                
        try{
            DataSource ds = (DataSource) AppUtil.getApplicationContext().getBean("setupDataSource");
            FormRow row = rows.get(0);
            
            con = ds.getConnection();
            
            if(!con.isClosed()){              
                
                UuidGenerator uuid = UuidGenerator.getInstance();
                String pId = uuid.getUuid();
                String parentId = formData.getPrimaryKeyValue();
                
                String activityName = row.getProperty("action_activity");
                String workflowName = row.getProperty("action_workflow");
                                
                String username = AppUtil.processHashVariable("#currentUser.username#", null, null, null);
                String name = AppUtil.processHashVariable("#currentUser.firstName# #currentUser.lastName#", null, null, null);
                String department = AppUtil.processHashVariable("#currentUser.department.name#", null, null, null);
                
                // String querySubject = row.getProperty("action_query_subject")!= null ?  row.getProperty("action_query_subject") : "";
                // String queryAddEmail = row.getProperty("action_additional_email")!= null ? row.getProperty("action_additional_email") : "";
                // String queryReason = row.getProperty("action_query_reason")!= null ? row.getProperty("action_query_reason") : "";

                String remarks = row.getProperty("action_remarks") != null ? row.getProperty("action_remarks") : ""; 
                String action_status = row.getProperty("status");
                String action_attachment = row.getProperty("action_attachment");
                String action_review_status = row.getProperty("action_review_status");
                String amend_reason = row.getProperty("action_amend_reason");
                
                String insertSql = "INSERT INTO app_fd_course_audit (dateCreated,dateModified,c_action_date,id,createdBy,createdByName,modifiedBy,modifiedByName,c_action_workflow,c_action_activity,c_parentId,c_action_name,c_action_department,c_action_remarks,c_status,c_action_attachment,c_action_review_status, c_action_amend_reason)"
                        + "VALUES (NOW(),NOW(),NOW(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
                 
                PreparedStatement stmtInsert = con.prepareStatement(insertSql);
                
                stmtInsert.setString(1, pId);
                stmtInsert.setString(2, username);
                stmtInsert.setString(3, name);
                stmtInsert.setString(4, username);
                stmtInsert.setString(5, name); 
                stmtInsert.setString(6, workflowName);
                stmtInsert.setString(7, activityName);
                stmtInsert.setString(8, parentId);
                stmtInsert.setString(9, name);
                stmtInsert.setString(10, department);
                stmtInsert.setString(11, remarks);
                stmtInsert.setString(12, action_status);
                stmtInsert.setString(13, action_attachment);
                stmtInsert.setString(14, action_review_status);
                stmtInsert.setString(15, amend_reason);
                
                //Execute SQL statement
                try{
                    String workflow_path = null;
                    if (workflowName.endsWith("Course Registration") || workflowName.endsWith("Course Amendment")){
                        workflow_path = "course_register";
                    }else if(workflowName.endsWith("Class Creation") || workflowName.endsWith("Class Amendment") || workflowName.endsWith("Class Cancellation")){
                        workflow_path = "course_class";
                    }else if(workflowName.endsWith("License Training Material")){
                        workflow_path = "course_ltm";
                    }
                    
                    String path = SetupManager.getBaseDirectory() + "app_formuploads/"+workflow_path+"/" + parentId+"/";
                    String newPath = SetupManager.getBaseDirectory() + "app_formuploads/course_audit/" +pId+"/";
                    
                    LogUtil.info("Course Audit Trail ---->","Checking if path exist: " + path);                    
                    
                    Path sourcePath = Paths.get(path, action_attachment);
                    Path destinationPath = Paths.get(newPath, action_attachment);
                    
                    
                    if (Files.exists(sourcePath)) {
                        LogUtil.info("Course Audit Trail ---->","Source path exist:" + path);      
                        if (!Files.exists(destinationPath.getParent())) {
                            LogUtil.info("Course Audit Trail ---->","Destination path not exist, Creating Folder"+ destinationPath);
                            
                            Files.createDirectories(destinationPath.getParent());
                        }
                                                
                        try {
                            LogUtil.info("Course Audit Trail ---->","Copying File");
                            Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                            LogUtil.info("Course Audit Trail ---->","Copy Successful");
                        } catch (IOException e) {
                            e.printStackTrace();
                            LogUtil.info("Course Audit Trail ---->","Error copy");
                        }
                    }else{
                        LogUtil.info("Course Audit Trail ---->","Source path doesn't exist:" + path); 
                        if (!Files.exists(destinationPath.getParent())) {
                            LogUtil.info("Course Audit Trail ---->","Path not exist, Creating Folder"+ destinationPath);
                            
                            Files.createDirectories(destinationPath.getParent());
                        }
                    }
                    
                    stmtInsert.executeUpdate();
                }catch (Exception ex){
                    
                }
                
            }
            
        }catch(Exception ex){
            
            LogUtil.error("HRDC - COURSE - Audit Trail ----->", ex, "Error storing using jdbc");
            
        }finally {
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception ex) {
                LogUtil.error("HRDC - COURSE - Audit Trail ----->", ex, "Error closing the jdbc connection");
            }
        } 
        
        return rows;
        
    }
    
    // Method to format date as per the required database format
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    @Override
    public String getName() {
        return ("HRDC - COURSE - Audit Trail Binder");
    }

    @Override
    public String getVersion() {
        return ("1.0.0");
    }

    @Override
    public String getDescription() {
        return ("To update audit trail record to database");
    }

    @Override
    public String getLabel() {
        return ("HRDC - COURSE - Audit Trail Binder");
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getFormId() {
        Form form = FormUtil.findRootForm(getElement());
        return form.getPropertyString(FormUtil.PROPERTY_ID);
    }

    @Override
    public String getTableName() {
        Form form = FormUtil.findRootForm(getElement());
        return form.getPropertyString(FormUtil.PROPERTY_TABLE_NAME);
    }
}