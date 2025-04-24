<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %> 
<%@ page import="ict.bean.UserBean" %>


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Borrow Fruits from Shops in <c:out value="${currentCity}"/></title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <link rel="stylesheet" type="text/css" href="https://cdn.datatables.net/1.13.6/css/jquery.dataTables.min.css">
    <script type="text/javascript" charset="utf8" src="https://cdn.datatables.net/1.13.6/js/jquery.dataTables.min.js"></script>
    <style>
        body { font-family: sans-serif; margin: 20px; background-color: #f4f4f4; }
        .container { background-color: #fff; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); max-width: 1000px; margin: auto; }
        h1 { color: #333; text-align: center; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; } 
        th { background-color: #f2f2f2; cursor: pointer; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .message, .error-message { padding: 10px; margin-bottom: 15px; border-radius: 4px; text-align: center; }
        .message { background-color: #d4edda; color: #155724; border: 1px solid #c3e6cb; }
        .error-message { background-color: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
        .back-link { display: block; text-align: center; margin-top: 20px; }
        .dataTables_wrapper { margin-top: 10px; margin-bottom: 20px;}
        input[type="number"] { width: 70px; padding: 5px; text-align: right; border: 1px solid #ccc; border-radius: 4px;}
        .lender-selection { margin-top: 20px; padding: 15px; background-color: #f0f0f0; border-radius: 5px; }
        .lender-selection label { font-weight: bold; margin-right: 10px;}
        .lender-selection select { padding: 8px; border: 1px solid #ccc; border-radius: 4px; min-width: 200px;}
        .submit-button { display: block; width: 200px; margin: 25px auto; padding: 12px; background-color: #28a745; color: white; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; text-align: center; }
        .submit-button:hover { background-color: #218838; }
        
        .lender-list { list-style: none; padding: 0; margin: 0; font-size: 0.9em; }
        .lender-list li { margin-bottom: 3px; }
        .lender-list .shop-name { font-weight: bold; }
        .lender-list .qty { color: green; }
        
    </style>
</head>
<body>
    <%@ include file="menu.jsp" %>

    
    <%
        UserBean currentUser = (UserBean) session.getAttribute("userInfo");
        if (currentUser == null || !"Bakery shop staff".equalsIgnoreCase(currentUser.getRole()) || currentUser.getShopId() == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=ShopStaffLoginRequired");
            return;
        }
    %>

    <div class="container">
        <h1>Borrow Fruits from Shops in <c:out value="${currentCity}"/></h1>
        <p>Enter quantities for the fruits you wish to borrow. Only fruits available from at least one other shop in your city are shown. Select ONE shop below to send the entire request to.</p>

        
        <c:if test="${not empty param.message}"> <div class="message success"><c:out value="${param.message}" /></div> </c:if>
        <c:if test="${not empty param.error}"> <div class="error-message"><c:out value="${param.error}" /></div> </c:if>
        <c:if test="${not empty errorMessage}"> <div class="error-message"><c:out value="${errorMessage}" /></div> </c:if>

        <form action="<c:url value='/batchBorrowFruit'/>" method="POST">
            <h2>Select Items and Quantities</h2>
            <table id="borrowItemsTable" class="display">
                <thead>
                    <tr>
                        <th>Fruit Name</th>
                        <th>Source Country</th>
                        <th>Available From (Shop & Qty)</th>
                        <th>Borrow Quantity</th>
                    </tr>
                </thead>
                <tbody>
                    
                    <c:forEach var="fruitInfo" items="${borrowableFruits}">
                        
                        
                        <c:if test="${not empty fruitInfo.lenderInfo}">
                            <tr>
                                <td><c:out value="${fruitInfo.fruitName}"/> (ID: <c:out value="${fruitInfo.fruitId}"/>)</td>
                                <td><c:out value="${fruitInfo.sourceCountry}"/></td>
                                <td>
                                    
                                    <ul class="lender-list">
                                        <c:forEach var="lender" items="${fruitInfo.lenderInfo}">
                                            <li>
                                                <span class="shop-name"><c:out value="${lender.shopName}"/>:</span>
                                                <span class="qty"><c:out value="${lender.quantity}"/></span>
                                            </li>
                                        </c:forEach>
                                    </ul>
                                </td>
                                <td>
                                    
                                    <input type="number" name="quantity_${fruitInfo.fruitId}" min="0" value="0">
                                </td>
                            </tr>
                        </c:if>
                        
                    </c:forEach>
                    
                    
                </tbody>
            </table>

             
             <div class="lender-selection">
                 <label for="lendingShopId">Borrow All Requested Items From:</label>
                 <select id="lendingShopId" name="lendingShopId" required>
                     <option value="">-- Select a Shop in <c:out value="${currentCity}"/> --</option>
                     <c:forEach var="lender" items="${potentialLenders}">
                         <option value="${lender.shop_id}">
                             <c:out value="${lender.shop_name}"/>
                         </option>
                     </c:forEach>
                      <c:if test="${empty potentialLenders}">
                           <option value="" disabled>No other shops found in your city.</option>
                     </c:if>
                 </select>
             </div>

            <button type="submit" class="submit-button" ${empty potentialLenders ? 'disabled' : ''}>Submit Borrow Request</button>
        </form>

    </div>

    <script>
        $(document).ready( function () {
            $('#borrowItemsTable').DataTable({
                "paging": false, 
                "order": [[ 0, "asc" ]], 
                "columnDefs": [ 
                    { "orderable": false, "targets": 2 }
                 ]
            });
        });
    </script>

</body>
</html>