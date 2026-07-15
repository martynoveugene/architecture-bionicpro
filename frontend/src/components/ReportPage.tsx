import React, { useEffect, useState } from 'react';

export default function ReportPage() {
  const [dates, setDates] = useState<string[]>([]);
  const [report, setReport] = useState<any>(null);

  const url = process.env.REACT_APP_AUTH_URL || 'http://localhost:8000';

  useEffect(() => {
    fetch(`${url}/reports`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => setDates(data));
  }, [url]);

  const loadReport = (day: string) => {
    fetch(`${url}/reports/${day}`, { credentials: 'include' })
      .then(res => res.json())
      .then(data => setReport(data));
  };

  return (
    <div>
      <h3>Список отчетов</h3>
      <ul>
        {dates.map(day => (
          <li key={day}>
            <button onClick={() => loadReport(day)}>Отчет за {day}</button>
          </li>
        ))}
      </ul>

      <h3>Детали отчета</h3>
      {report ? (
        <div>
          <p>Пользователь: {report.customerId}</p>
          <p>Дата: {report.day}</p>
          <p>Температура: {report.avg_temperature}</p>
          <p>Мин. заряд: {report.min_chargeLevel}</p>
          <p>Макс. заряд: {report.max_chargeLevel}</p>
        </div>
      ) : (
        <p>Нажмите на кнопку выше</p>
      )}
    </div>
  );
}
