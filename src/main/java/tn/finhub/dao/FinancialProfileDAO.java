package tn.finhub.dao;

import tn.finhub.model.FinancialProfile;

public interface FinancialProfileDAO {

    FinancialProfile findByUserId(int userId);

    void create(FinancialProfile profile);

    void update(FinancialProfile profile);
}
