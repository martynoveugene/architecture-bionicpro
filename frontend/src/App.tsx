import React, { useEffect, useState } from 'react';
import ReportPage from './components/ReportPage';

const App: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    fetch(`${process.env.REACT_APP_AUTH_URL}/api/user-info`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => {
        if (data.authenticated) {
          setUser(data);
          setLoading(false);
        } else {
          window.location.href = data.loginUrl;
        }
      })
      .catch(err => {
        console.error("Ошибка сети:", err);
        setLoading(false);
      });
  }, []);

  const handleLogout = () => {
    window.location.href = `${process.env.REACT_APP_AUTH_URL}/logout`;
  };

  if (loading) return <div style={{ padding: '20px', textAlign: 'center' }}>Загрузка ...</div>;

  return (
    <div className="App">
      <header style={{ display: 'flex', justifyContent: 'space-between', padding: '10px', background: '#f5f5f5', alignItems: 'center' }}>
        <span>Вы вошли как: <b>{user?.name || user?.email}</b></span>

      </header>

      <ReportPage />
    </div>
  );
};

export default App;
