import React, { useEffect, useState } from 'react';

export default function ReportPage() {
  const [dates, setDates] = useState<string[]>([]);
  const [downloadingDay, setDownloadingDay] = useState<string | null>(null);

  const url = process.env.REACT_APP_AUTH_URL || 'http://localhost:8000';

  useEffect(() => {
    fetch(`${url}/reports`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => setDates(data));
  }, [url]);

const downloadReport = async (day: string) => {
  setDownloadingDay(day);
  try {
    const res = await fetch(`${url}/reports/${day}`, { credentials: 'include' });

    if (!res.ok) throw new Error('Ошибка сервера');

    const blob = await res.blob();

    const downloadUrl = window.URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.href = downloadUrl;
    link.setAttribute('download', `report-${day}.txt`);
    document.body.appendChild(link);
    link.click();

    link.remove();
    window.URL.revokeObjectURL(downloadUrl);
  } catch (err) {
    alert('Ошибка при скачивании файла отчета');
  } finally {
    setDownloadingDay(null);
  }
};

  return (
    <div className="dashboard-container" style={{ maxWidth: '600px', margin: '0 auto' }}>
      <div className="sidebar" style={{ width: '100%' }}>
        <div className="dashboard-title">Выгрузка отчетов</div>
        <table className="report-table">
          <thead>
            <tr>
              <th>Дата отчета</th>
              <th style={{ textAlign: 'right' }}>Действие</th>
            </tr>
          </thead>
          <tbody>
            {dates.map(day => (
              <tr key={day} className="table-row">
                <td><strong>Отчет за {day}</strong></td>
                <td style={{ textAlign: 'right' }}>
                  <button
                    className="btn-action btn-download"
                    disabled={downloadingDay === day}
                    onClick={() => downloadReport(day)}
                  >
                    {downloadingDay === day ? 'Генерация...' : '⬇ Скачать (.txt)'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
