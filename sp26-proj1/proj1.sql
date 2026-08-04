-- Before running drop any existing views
DROP VIEW IF EXISTS q0;
DROP VIEW IF EXISTS q1i;
DROP VIEW IF EXISTS q1ii;
DROP VIEW IF EXISTS q1iii;
DROP VIEW IF EXISTS q1iv;
DROP VIEW IF EXISTS q2i;
DROP VIEW IF EXISTS q2ii;
DROP VIEW IF EXISTS q2iii;
DROP VIEW IF EXISTS q3i;
DROP VIEW IF EXISTS q3ii;
DROP VIEW IF EXISTS q3iii;
DROP VIEW IF EXISTS q4i;
DROP VIEW IF EXISTS q4ii;
DROP VIEW IF EXISTS q4iii;
DROP VIEW IF EXISTS q4iv;
DROP VIEW IF EXISTS q4v;

-- Question 0
CREATE VIEW q0(era)
AS
  SELECT MAX(era) -- replace this line
  FROM pitching
;

-- Question 1i
CREATE VIEW q1i(namefirst, namelast, birthyear)
AS
  SELECT namefirst, namelast, birthyear
  FROM people
  WHERE weight > 300
;

-- Question 1ii
CREATE VIEW q1ii(namefirst, namelast, birthyear)
AS
  SELECT namefirst, namelast, birthyear
  FROM people
  WHERE namefirst LIKE '% %'
  ORDER BY namefirst, namelast
;

-- Question 1iii
CREATE VIEW q1iii(birthyear, avgheight, count)
AS
  SELECT birthyear, AVG(height), COUNT(playerid)
  FROM people
  GROUP BY birthyear
  ORDER BY birthyear
;

-- Question 1iv
CREATE VIEW q1iv(birthyear, avgheight, count)
AS
  SELECT birthyear, avgheight, count
  FROM q1iii
  WHERE avgheight > 70
  ORDER BY birthyear -- can have or not have
;

-- Question 2i
CREATE VIEW q2i(namefirst, namelast, playerid, yearid)
AS
  SELECT namefirst, namelast, p.playerid, yearid
  FROM people p, halloffame h
  WHERE p.playerid = h.playerid
  AND h.inducted = 'Y'
  ORDER BY yearid DESC, p.playerid
;

-- Question 2ii
CREATE VIEW q2ii(namefirst, namelast, playerid, schoolid, yearid)
AS
  SELECT q.namefirst, q.namelast, q.playerid, s.schoolid, q.yearid
  FROM q2i q
  INNER JOIN collegeplaying c ON q.playerid = c.playerid
  INNER JOIN schools s ON c.schoolid = s.schoolid
  WHERE s.schoolState = 'CA'
  ORDER BY q.yearid DESC, s.schoolid, q.playerid
;

-- Question 2iii
CREATE VIEW q2iii(playerid, namefirst, namelast, schoolid)
AS
  SELECT q.playerid, q.namefirst, q.namelast, c.schoolid
  FROM q2i q
  LEFT OUTER JOIN collegeplaying c ON q.playerid = c.playerid
  ORDER BY q.playerid DESC, c.schoolid
;

-- Question 3i
CREATE VIEW q3i(playerid, namefirst, namelast, yearid, slg)
AS
  SELECT p.playerid, p.namefirst, p.namelast, bs.yearid, bs.slg
  FROM people p,
  (SELECT playerid, yearid, ((H + H2B + 2 * H3B + 3 * HR) * 1.0 / AB) AS slg
    FROM batting b
    WHERE AB > 50
    ORDER BY slg DESC
    LIMIT 10) AS bs
  WHERE p.playerid = bs.playerid
  ORDER BY bs.slg DESC, bs.yearid, p.playerid
;

-- Question 3ii
CREATE VIEW q3ii(playerid, namefirst, namelast, lslg)
AS
  SELECT p.playerid, p.namefirst, p.namelast, ((sh + s2b + 2 * s3b + 3 * shr) * 1.0 / sab) AS lslg
  FROM people p,
  (SELECT playerid, SUM(H) AS sh, SUM(H2B) AS s2b, SUM(H3B) AS s3b, SUM(HR) AS shr, SUM(AB) AS sab
    FROM batting b
    GROUP BY playerid
    HAVING sab > 50) AS bs
  WHERE p.playerid = bs.playerid
  ORDER BY lslg DESC, p.playerid
  LIMIT 10
;

-- Question 3iii
CREATE VIEW q3iii(namefirst, namelast, lslg)
AS
  SELECT namefirst, namelast, lslg
  FROM people p,
  (SELECT playerid, SUM(AB) AS sab, ((SUM(h) + SUM(h2b) + 2 * SUM(h3b) + 3 * SUM(hr)) * 1.0 / SUM(ab)) AS lslg
    FROM batting b
    GROUP BY playerid
    HAVING sab > 50) AS bs
  WHERE p.playerid = bs.playerid
  AND bs.lslg > (
    SELECT (SUM(h) + SUM(h2b) + 2 * SUM(h3b) + 3 * SUM(hr)) * 1.0 / SUM(ab)
    FROM batting
    WHERE playerid = 'mayswi01'
  )
  ORDER BY lslg DESC, p.playerid
;

-- Question 4i
CREATE VIEW q4i(yearid, min, max, avg)
AS
  SELECT yearid, MIN(salary), MAX(salary), AVG(salary)
  FROM salaries
  GROUP BY yearid
  ORDER BY yearid
;

-- Question 4ii
CREATE VIEW q4ii(binid, low, high, count)
AS
   WITH w(mins, maxs, width) AS
   (SELECT MIN(salary), MAX(salary), (MAX(salary) - MIN(salary)) / 10.0 AS width
   FROM salaries
   WHERE yearid = 2016
   )

   , b_s(id, salary, bid, width, mins) AS
   (SELECT
      id,
      salary,
      CASE
        WHEN salary = w.maxs THEN 9
        ELSE CAST((salary - w.mins)/width AS INT)
        END AS bid,
       width,
       mins
      FROM salaries, w
      WHERE yearid = 2016
   )

   SELECT b.binid,
     mins + b.binid * width AS low,
     mins + (b.binid+1) * width AS high,
     COUNT(id)
   FROM binids b LEFT OUTER JOIN b_s -- b_S.bid may be not exist and will be NULL
   ON b.binid = b_s.bid
   GROUP BY b.binid
   ORDER BY b.binid
;

-- Question 4iii
CREATE VIEW q4iii(yearid, mindiff, maxdiff, avgdiff)
AS
  WITH basic(yearid, mins, maxs, avgs) AS
  (SELECT yearid, MIN(salary), MAX(salary), AVG(salary)
  FROM salaries
  GROUP BY yearid
  ORDER BY yearid
  ),
  diff_basic(yearid, mins, maxs, avgs, pmins, pmaxs, pavgs) AS
  (SELECT b1.yearid, b1.mins, b1.maxs, b1.avgs, b2.mins, b2.maxs, b2.avgs
  FROM basic b1, basic b2
  WHERE b1.yearid = b2.yearid + 1 -- make sure not print the first yearid in s1 table
  )
  SELECT yearid, mins - pmins, maxs - pmaxs, avgs - pavgs
  FROM diff_basic
  ORDER BY yearid
;

-- Question 4iv
CREATE VIEW q4iv(playerid, namefirst, namelast, salary, yearid)
AS
  WITH maxSalary(playerid, salary, yearid) AS
  (SELECT playerid, salary, yearid
     FROM salaries
     WHERE salary = (SELECT MAX(salary) FROM salaries WHERE yearid = 2000)
     AND yearid = 2000

   UNION ALL

   SELECT playerid, salary, yearid
     FROM salaries
     WHERE salary = (SELECT MAX(salary) FROM salaries WHERE yearid = 2001)
     AND yearid = 2001
  )

  SELECT ms.playerid, nameFirst, nameLast, salary, yearid
  FROM maxSalary ms INNER JOIN people p
  ON ms.playerid = p.playerid
  ORDER BY yearid
;
-- Question 4v
CREATE VIEW q4v(team, diffAvg) AS
  WITH salaryas AS
  (SELECT a.teamid, a.playerid, salary
  FROM allstarfull a INNER JOIN salaries s
  ON a.playerid = s.playerid AND a.yearid = s.yearid AND a.teamid = s.teamid
  WHERE a.yearid = 2016
  )
  SELECT teamid, MAX(salary) - MIN(salary)
  FROM salaryas sas
  GROUP BY sas.teamid
;

