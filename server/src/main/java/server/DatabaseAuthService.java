package server;

public class DatabaseAuthService implements AuthService {
    @Override
    public String getNicknameByLoginAndPassword (String login, String password) {
        return DataBase.getNameAuthUser(login,password);
    }

    @Override
    public boolean registration (String login, String password, String nickname) {
        return false;
    }

    @Override
    public boolean changeNickname (String currentNickname, String newNickname) {
        return false;
    }


    public boolean changeNickname (Integer id,String newName) {
        return DataBase.updateUserName(id, newName);
    }
}
