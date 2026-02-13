package tn.finhub.service;

import tn.finhub.dao.FinancialProfileDAO;
import tn.finhub.dao.impl.FinancialProfileDAOImpl;
import tn.finhub.model.FinancialProfile;

public class FinancialProfileService {

    private final FinancialProfileDAO dao = new FinancialProfileDAOImpl();

    public FinancialProfile getByUserId(int userId) {
        return dao.findByUserId(userId);
    }

    public void ensureProfile(int userId) {
        FinancialProfile profile = dao.findByUserId(userId);
        if (profile == null) {
            profile = new FinancialProfile(
                    userId,
                    0.0,
                    0.0,
                    0.0,
                    "LOW",
                    "TND",
                    false);
            dao.create(profile);
        }
    }

    public boolean isProfileCompleted(int userId) {
        FinancialProfile profile = dao.findByUserId(userId);
        return profile != null && profile.isProfileCompleted();
    }

    public void createProfile(FinancialProfile profile) {
        dao.create(profile);
    }

    public void updateProfile(FinancialProfile profile) {
        dao.update(profile);
    }
}
