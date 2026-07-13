import React, { useEffect, useState } from 'react';
import ReportPage from './components/ReportPage';

const App: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    fetch(`${process.env.REACT_APP_API_URL}/api/user-info`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        if (data.authenticated) {
          setUser(data);
        } else {
          window.location.href = data.loginUrl;
        }
        setLoading(false);
      });
  }, []);

  const handleLogout = () => {
    window.location.href = `${process.env.REACT_APP_API_URL}/logout`;
  };

  if (loading) return <div>Загрузка ...</div>;

  return (
      <div className="App">
        <header style={{ display: 'flex', justifyContent: 'space-between', padding: '10px', background: '#f5f5f5' }}>
          <span>Вы вошли как: <b>{user?.name || user?.email}</b></span>
          <button onClick={handleLogout} style={{ cursor: 'pointer', padding: '5px 10px' }}>
            Выйти
          </button>
        </header>

        <ReportPage />
      </div>
    );
};

export default App;
