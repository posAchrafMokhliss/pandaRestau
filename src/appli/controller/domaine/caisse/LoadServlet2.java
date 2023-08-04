package appli.controller.domaine.caisse;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import appli.controller.domaine.util_erp.ContextAppli;
import appli.model.domaine.util_srv.job.JobJourneeCron;
import appli.model.domaine.util_srv.job.JobSaveLocalDbCron;
import framework.model.util.audit.ReplicationGenerationEventListener;

public class LoadServlet2 extends HttpServlet {
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		// Démarrer les jobs manuels quartz
		JobJourneeCron instance = JobJourneeCron.getInstance();
		JobSaveLocalDbCron instanceDb = JobSaveLocalDbCron.getInstance();
		
		// -------------------------------------------------------
		// Not Cloud master
		if(!ContextAppli.IS_CLOUD_MASTER() && !ContextAppli.IS_FULL_CLOUD()) {
			System.out.println("************************* Lancement cron synchronisation CLOUD to LOCAL *************************");
			if(!ContextAppli.IS_CLOUD_MASTER() && ReplicationGenerationEventListener._IS_LOCAL_SYNCHRO_INSTANCE) {
				instance.init_job_postSyncToRemote();
			}
			
			System.out.println("************************* Lancement cron sauvegarde db *************************");
			instanceDb.init_job_save_db();
		}
		
		System.out.println("************************* Lancement cron ouverture journée *************************");
		instance.init_job_journee();
	}
}
